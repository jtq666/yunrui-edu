$ErrorActionPreference = "Stop"

# 这份脚本只验证简历中最重要的并发、幂等、异步和权限声明。
# API 直接访问 Spring Boot，避免把前端代理层当成后端测试结果。
$base = "http://localhost:18080/api"
$run = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$results = [ordered]@{}

function Check($name, $condition, $detail) {
    $results[$name] = [ordered]@{ passed = [bool]$condition; detail = $detail }
    if (-not $condition) { throw "验证失败: $name ($detail)" }
}

function Register($prefix) {
    Invoke-RestMethod "$base/auth/register" -Method Post -ContentType "application/json" -Body (@{
        username = "$prefix$run"; password = "12345678"; nickname = $prefix
    } | ConvertTo-Json -Compress)
}

function AuthHeaders($token) { @{ Authorization = "Bearer $token" } }

$courses = Invoke-RestMethod "$base/courses"
$course = $courses | Where-Object id -eq 2002 | Select-Object -First 1
$section = (Invoke-RestMethod "$base/courses/2002").sections | Select-Object -First 1
Check "基础课程接口" ($null -ne $course -and $null -ne $section) "课程和章节可读取"

# 1. 同一个用户并发领取同一张券：唯一约束 + 分布式锁/事务只能成功一次。
$user = Register "claim-race-$run-"
$headers = AuthHeaders $user.token
$claimRace = 1..10 | ForEach-Object -Parallel {
    $body = @{ userId = $using:user.userId; couponId = 3001 }
    try {
        $value = Invoke-RestMethod "$using:base/coupons/claim" -Method Post -Headers $using:headers -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
        [pscustomobject]@{ ok = $true; status = 200; value = $value }
    } catch {
        $status = 0
        if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode.value__ }
        [pscustomobject]@{ ok = $false; status = $status; value = $null }
    }
} -ThrottleLimit 10
$claimSuccess = @($claimRace | Where-Object ok).Count
Check "优惠券同用户并发幂等" ($claimSuccess -eq 1) "10 次并发请求成功 $claimSuccess 次"

# 同一库存下多个不同用户并发领取，验证条件扣减没有把库存多扣或少扣。
$stockUsers = 1..20 | ForEach-Object { Register "stock-race-$run-$_-" }
$stockClaims = $stockUsers | ForEach-Object -Parallel {
    $localHeaders = @{ Authorization = "Bearer $($_.token)" }
    $body = @{ userId = $_.userId; couponId = 3003 }
    try {
        Invoke-RestMethod "$using:base/coupons/claim" -Method Post -Headers $localHeaders -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress) | Out-Null
        [pscustomobject]@{ ok = $true }
    } catch {
        [pscustomobject]@{ ok = $false }
    }
} -ThrottleLimit 20
$stockSuccess = @($stockClaims | Where-Object ok).Count
Check "优惠券库存并发扣减" ($stockSuccess -eq 20) "20 个不同用户并发领取成功 $stockSuccess 次"

# 2. 订单支付回调并发：同一个 transactionId 只能有一个 firstProcessed=true。
$order = Invoke-RestMethod "$base/orders" -Method Post -Headers $headers -ContentType "application/json" -Body (@{ userId = $user.userId; courseId = 2002 } | ConvertTo-Json -Compress)
$orderDetail = Invoke-RestMethod "$base/orders/$($order.id)" -Headers $headers
$transactionId = "resume-pay-$run"
$paymentRace = 1..10 | ForEach-Object -Parallel {
    $body = @{ orderId = $using:order.id; transactionId = $using:transactionId; paidAmount = [decimal]$using:orderDetail.payableAmount }
    try {
        $value = Invoke-RestMethod "$using:base/payments/callback" -Method Post -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress)
        [pscustomobject]@{ ok = $true; firstProcessed = [bool]$value.firstProcessed }
    } catch {
        [pscustomobject]@{ ok = $false; firstProcessed = $false }
    }
} -ThrottleLimit 10
$firstCount = @($paymentRace | Where-Object firstProcessed).Count
$paymentErrors = @($paymentRace | Where-Object { -not $_.ok }).Count
$paid = $false
for ($i = 0; $i -lt 24; $i++) {
    Start-Sleep -Milliseconds 500
    $orderAfter = Invoke-RestMethod "$base/orders/$($order.id)" -Headers $headers
    if ($orderAfter.status -eq "PAID") { $paid = $true; break }
}
Check "支付回调并发幂等" ($firstCount -eq 1 -and $paymentErrors -eq 0 -and $paid) "10 次回调 firstProcessed=$firstCount，错误=$paymentErrors，最终状态=$($orderAfter.status)"

# 3. 消息最终一致性：支付后由异步消息创建课程权限。
$entitlement = @()
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 500
    $entitlement = @(Invoke-RestMethod "$base/users/$($user.userId)/courses" -Headers $headers)
    if (@($entitlement | Where-Object { [int64]$_.courseId -eq 2002 -and [int64]$_.orderId -eq [int64]$order.id }).Count -ge 1) { break }
}
Check "支付后异步发课" (@($entitlement | Where-Object { [int64]$_.courseId -eq 2002 -and [int64]$_.orderId -eq [int64]$order.id }).Count -ge 1) "课程权限在异步处理后可查询"

# 4. 进度合并：持续上报只保留最后一次进度，停止后再持久化。
$progressReports = @(10, 20, 30)
foreach ($position in $progressReports) {
    $body = @{ userId = $user.userId; courseId = 2002; sectionId = $section.id; positionSeconds = $position; durationSeconds = [int]$section.durationSeconds }
    Invoke-RestMethod "$base/learning-progress" -Method Post -Headers $headers -ContentType "application/json" -Body ($body | ConvertTo-Json -Compress) | Out-Null
}
Start-Sleep -Seconds 2
$saved = @()
$savedSection = $null
for ($i = 0; $i -lt 36; $i++) {
    Start-Sleep -Milliseconds 500
    $saved = @(Invoke-RestMethod "$base/learning-progress/users/$($user.userId)/courses/2002" -Headers $headers)
    $savedSection = $saved | Where-Object { [int64]$_.sectionId -eq [int64]$section.id } | Select-Object -First 1
    if ($null -ne $savedSection) { break }
}
Check "进度合并写" ($null -ne $savedSection -and [int]$savedSection.positionSeconds -eq 30) "最终 position=$($savedSection.positionSeconds)"

# 5. 数据权限：另一个用户不能读取该用户的课程。
$other = Register "access-race-$run-"
$crossUserRejected = $false
try {
    Invoke-RestMethod "$base/users/$($user.userId)/courses" -Headers (AuthHeaders $other.token) | Out-Null
} catch {
    $crossUserRejected = $true
}
Check "用户数据隔离" $crossUserRejected "第二个用户读取第一个用户课程被拒绝"

# 6. 播放凭证：已购买用户可获得短期 ticket，响应支持 Range；未购买用户不能拿 ticket。
$ticket = Invoke-RestMethod "$base/videos/sections/$($section.id)/ticket" -Method Post -Headers $headers -ContentType "application/json" -Body (@{ userId = $user.userId } | ConvertTo-Json -Compress)
$range = Invoke-WebRequest "$base/videos/sections/$($section.id)?ticket=$([uri]::EscapeDataString($ticket.ticket))" -Headers @{ Range = "bytes=0-1023" } -UseBasicParsing
$unownedRejected = $false
try {
    Invoke-RestMethod "$base/videos/sections/$($section.id)/ticket" -Method Post -Headers (AuthHeaders $other.token) -ContentType "application/json" -Body (@{ userId = $other.userId } | ConvertTo-Json -Compress) | Out-Null
} catch {
    $unownedRejected = $true
}
Check "视频授权和分段读取" ($range.StatusCode -eq 206 -and $unownedRejected) "已购买用户返回 206，未购买用户申请 ticket 被拒绝"

$results | ConvertTo-Json -Depth 5

