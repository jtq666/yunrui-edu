$ErrorActionPreference = "Stop"
$base = "http://localhost:18082/api"
$run = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$checks = [ordered]@{}

function Check($name, $condition) {
    $checks[$name] = [bool]$condition
    if (-not $condition) { throw "Check failed: $name" }
}

function Register($name) {
    Invoke-RestMethod "$base/auth/register" -Method Post -ContentType "application/json" -Body (@{
        username = "$name$run"; password = "12345678"; nickname = $name
    } | ConvertTo-Json)
}

function Headers($auth) { @{ Authorization = "Bearer $($auth.token)" } }

$page = Invoke-WebRequest "http://localhost:18082/index.html" -UseBasicParsing
Check "frontend_page" ($page.StatusCode -eq 200 -and $page.Content -match '/src/main.js')

$courses = Invoke-RestMethod "$base/courses"
Check "course_list" ($courses.Count -ge 2)
$aiCourse = $courses | Where-Object id -eq 2002
$javaCourse = $courses | Where-Object id -eq 2001
Check "course_detail" ($aiCourse.sections.Count -ge 1)

$userA = Register "flow-a-"
$headersA = Headers $userA
$loginA = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType "application/json" -Body (@{
    username = "flow-a-$run"; password = "12345678"
} | ConvertTo-Json)
Check "register_and_login" ($loginA.userId -eq $userA.userId)

Invoke-RestMethod "$base/coupons/claim" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;couponId=3001}|ConvertTo-Json) | Out-Null
Invoke-RestMethod "$base/coupons/claim" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;couponId=3002}|ConvertTo-Json) | Out-Null
$planA = Invoke-RestMethod "$base/coupons/best-plan" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;orderAmount=$aiCourse.price}|ConvertTo-Json)
Check "coupon_combination" ([decimal]$planA.discountAmount -eq 70 -and [decimal]$planA.payableAmount -eq 329)

$orderA = Invoke-RestMethod "$base/orders" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;courseId=2002}|ConvertTo-Json)
$detailA = Invoke-RestMethod "$base/orders/$($orderA.id)" -Headers $headersA
Check "create_order" ($detailA.status -eq "PENDING_PAYMENT" -and [decimal]$detailA.payableAmount -eq 329)

$wrongAmountRejected = $false
try {
    Invoke-RestMethod "$base/payments/callback" -Method Post -ContentType "application/json" -Body (@{orderId=$orderA.id;transactionId="wrong-$run";paidAmount=1}|ConvertTo-Json) | Out-Null
} catch { $wrongAmountRejected = $_.Exception.Response.StatusCode.value__ -eq 400 }
Check "wrong_payment_amount_rejected" $wrongAmountRejected

$payment = Invoke-RestMethod "$base/payments/callback" -Method Post -ContentType "application/json" -Body (@{orderId=$orderA.id;transactionId="pay-$run";paidAmount=329}|ConvertTo-Json)
$duplicatePayment = Invoke-RestMethod "$base/payments/callback" -Method Post -ContentType "application/json" -Body (@{orderId=$orderA.id;transactionId="pay-$run";paidAmount=329}|ConvertTo-Json)
Check "payment_idempotency" ($payment.firstProcessed -and -not $duplicatePayment.firstProcessed)

$deadline = (Get-Date).AddSeconds(12)
do {
    Start-Sleep -Milliseconds 500
    $myCourses = Invoke-RestMethod "$base/users/$($userA.userId)/courses" -Headers $headersA
} while ($myCourses.Count -eq 0 -and (Get-Date) -lt $deadline)
Check "message_grants_course" ($myCourses.Count -eq 1 -and $myCourses[0].courseId -eq 2002)

$section = $aiCourse.sections[0]
$firstProgress = Invoke-RestMethod "$base/learning-progress" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;courseId=2002;sectionId=$section.id;positionSeconds=100;durationSeconds=$section.durationSeconds}|ConvertTo-Json)
$latestProgress = Invoke-RestMethod "$base/learning-progress" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{userId=$userA.userId;courseId=2002;sectionId=$section.id;positionSeconds=900;durationSeconds=$section.durationSeconds}|ConvertTo-Json)
Check "progress_keeps_latest_report" ($firstProgress.accepted -and $latestProgress.accepted)
Start-Sleep -Seconds 6
$savedProgress = Invoke-RestMethod "$base/learning-progress/users/$($userA.userId)/courses/2002" -Headers $headersA
Check "progress_persisted" ($savedProgress.Count -eq 1 -and $savedProgress[0].positionSeconds -eq 900)

$pointEvent = "point-$run"
$point1 = Invoke-RestMethod "$base/points/events" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{eventId=$pointEvent;userId=$userA.userId;points=30;reason="section-completed"}|ConvertTo-Json)
$point2 = Invoke-RestMethod "$base/points/events" -Method Post -Headers $headersA -ContentType "application/json" -Body (@{eventId=$pointEvent;userId=$userA.userId;points=30;reason="duplicate"}|ConvertTo-Json)
Check "points_idempotency" ($point1.accepted -and -not $point2.accepted)
Start-Sleep -Seconds 3
$season = Get-Date -Format "yyyyMM"
$rank = Invoke-RestMethod "$base/points/boards/$season/users/$($userA.userId)" -Headers $headersA
Check "points_ranking" ($rank.points -ge 30 -and $rank.rank -ge 1)

$userB = Register "flow-b-"
$headersB = Headers $userB
Invoke-RestMethod "$base/coupons/claim" -Method Post -Headers $headersB -ContentType "application/json" -Body (@{userId=$userB.userId;couponId=3003}|ConvertTo-Json) | Out-Null
$orderB = Invoke-RestMethod "$base/orders" -Method Post -Headers $headersB -ContentType "application/json" -Body (@{userId=$userB.userId;courseId=2001}|ConvertTo-Json)
$beforeCancel = Invoke-RestMethod "$base/orders/$($orderB.id)" -Headers $headersB
Invoke-RestMethod "$base/orders/$($orderB.id)/cancel" -Method Post -Headers $headersB -ContentType "application/json" -Body (@{userId=$userB.userId}|ConvertTo-Json) | Out-Null
$afterCancel = Invoke-RestMethod "$base/orders/$($orderB.id)" -Headers $headersB
$releasedPlan = Invoke-RestMethod "$base/coupons/best-plan" -Method Post -Headers $headersB -ContentType "application/json" -Body (@{userId=$userB.userId;orderAmount=$javaCourse.price}|ConvertTo-Json)
Check "cancel_releases_coupon" ([decimal]$beforeCancel.discountAmount -eq 15 -and $afterCancel.status -eq "CLOSED" -and [decimal]$releasedPlan.discountAmount -eq 15)

$crossUserRejected = $false
try { Invoke-RestMethod "$base/users/$($userA.userId)/orders" -Headers $headersB | Out-Null }
catch { $crossUserRejected = $_.Exception.Response.StatusCode.value__ -eq 400 }
Check "cross_user_access_rejected" $crossUserRejected

$checks | ConvertTo-Json
