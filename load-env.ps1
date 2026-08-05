Get-Content (Join-Path $PSScriptRoot ".env") | ForEach-Object {
    if ($_ -match '^\s*([^#=\s][^=]*)\s*=\s*(.*)\s*$') {
        Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
    }
}
