$files = Get-ChildItem -Path ".\src\test\java" -Recurse -Filter *.java

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $original = $content
    
    $content = $content -replace "com\.holaho\.intern\.enums\.", "com.holaho.intern.shared.enums."
    $content = $content -replace "com\.holaho\.intern\.security\.", "com.holaho.intern.shared.security."
    $content = $content -replace "com\.holaho\.intern\.dto\.", "com.holaho.intern.shared.dto."
    $content = $content -replace "com\.holaho\.intern\.exception\.", "com.holaho.intern.shared.exception."
    $content = $content -replace "com\.holaho\.intern\.elasticsearch\.", "com.holaho.intern.shared.elasticsearch."
    $content = $content -replace "com\.holaho\.intern\.mapper\.", "com.holaho.intern.shared.mapper."
    
    # Also verify if any entities inside the test folder miss base entity import? Probably tests do not declare entities. But just in case:
    if (($content -match "extends BaseEntity") -and ($content -notmatch "import com\.holaho\.intern\.shared\.entity\.BaseEntity;")) {
        $content = $content -replace "(package [^;]+;)", "`$1`r`n`r`nimport com.holaho.intern.shared.entity.BaseEntity;"
    }
    
    if ($content -cne $original) {
        Write-Host "Updating $($f.FullName)"
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
    }
}
Write-Host "Done test fixing"
