# PowerShell script to test the forgot password and reset password flow

$baseUrl = "http://localhost:8080/api/v1/auth"
$email = "test.user@example.com" # adjust to a real test user if needed

Write-Host "1. Testing Forgot Password with email: $email"
try {
    $forgotResponse = Invoke-RestMethod -Uri "$baseUrl/forgot-password" -Method Post -ContentType "application/json" -Body "{ `"email`": `"$email`" }"
    Write-Host "Forgot Password Response: " $forgotResponse.message
} catch {
    Write-Host "Error in Forgot Password: $_"
}

Write-Host "`nNote: Check the database or console logs to get the generated token for the next step."
Write-Host "2. Testing Reset Password"
Write-Host "Please assign the generated token to a variable `$token and run the following manually:"
Write-Host "`$resetBody = @{ token = `$token; newPassword = `"NewSecureP@ssw0rd`"; confirmPassword = `"NewSecureP@ssw0rd`" } | ConvertTo-Json"
Write-Host "Invoke-RestMethod -Uri `"$baseUrl/reset-password`" -Method Post -ContentType `"application/json`" -Body `$resetBody"
