$ErrorActionPreference = "Stop"
$base = "http://localhost:18080"
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$health = Invoke-RestMethod "$base/actuator/health"
$auth = Invoke-RestMethod "$base/api/auth/register" -Method Post -ContentType "application/json" -Body (@{username="smoke$suffix";password="test123456";nickname="smoke-user"}|ConvertTo-Json)
$headers = @{Authorization="Bearer $($auth.token)"}; $userId = $auth.userId
Invoke-RestMethod "$base/api/coupons/claim" -Method Post -Headers $headers -ContentType "application/json" -Body (@{userId=$userId;couponId=3001}|ConvertTo-Json) | Out-Null
Invoke-RestMethod "$base/api/coupons/claim" -Method Post -Headers $headers -ContentType "application/json" -Body (@{userId=$userId;couponId=3002}|ConvertTo-Json) | Out-Null
$order = Invoke-RestMethod "$base/api/orders" -Method Post -Headers $headers -ContentType "application/json" -Body (@{userId=$userId;courseId=2002}|ConvertTo-Json)
$detail = Invoke-RestMethod "$base/api/orders/$($order.id)" -Headers $headers
$payment = Invoke-RestMethod "$base/api/payments/callback" -Method Post -ContentType "application/json" -Body (@{orderId=$order.id;transactionId="pay-$suffix";paidAmount=$detail.payableAmount}|ConvertTo-Json)
$duplicate = Invoke-RestMethod "$base/api/payments/callback" -Method Post -ContentType "application/json" -Body (@{orderId=$order.id;transactionId="pay-$suffix";paidAmount=$detail.payableAmount}|ConvertTo-Json)
Start-Sleep -Seconds 2
$courses = Invoke-RestMethod "$base/api/users/$userId/courses" -Headers $headers
$progress = Invoke-RestMethod "$base/api/learning-progress" -Method Post -Headers $headers -ContentType "application/json" -Body (@{userId=$userId;courseId=2002;sectionId=2201;positionSeconds=750;durationSeconds=1500}|ConvertTo-Json)
Start-Sleep -Seconds 6
$savedProgress = Invoke-RestMethod "$base/api/learning-progress/users/$userId/courses/2002" -Headers $headers
$point = Invoke-RestMethod "$base/api/points/events" -Method Post -Headers $headers -ContentType "application/json" -Body (@{eventId="point-$suffix";userId=$userId;points=30;reason="section-completed"}|ConvertTo-Json)
$duplicatePoint = Invoke-RestMethod "$base/api/points/events" -Method Post -Headers $headers -ContentType "application/json" -Body (@{eventId="point-$suffix";userId=$userId;points=30;reason="duplicate"}|ConvertTo-Json)
Start-Sleep -Seconds 3
$season = Get-Date -Format "yyyyMM"; $rank = Invoke-RestMethod "$base/api/points/boards/$season/users/$userId" -Headers $headers
$result = [ordered]@{health=$health.status;userId=$userId;orderId=$order.id;originalAmount=$detail.originalAmount;discountAmount=$detail.discountAmount;payableAmount=$detail.payableAmount;paymentProcessed=$payment.firstProcessed;duplicatePaymentBlocked=(-not $duplicate.firstProcessed);courseGranted=($courses.Count -gt 0);progressBuffered=$progress.accepted;progressPersisted=($savedProgress.Count -gt 0);pointAccepted=$point.accepted;duplicatePointBlocked=(-not $duplicatePoint.accepted);points=$rank.points;rank=$rank.rank}
$result | ConvertTo-Json
if ($result.Values -contains $false -or $health.status -ne "UP") { throw "Smoke test failed" }
