$files = Get-ChildItem -Path ".\src\main\java" -Recurse -Filter *.java

foreach ($f in $files) {
    if ($f.FullName -match "\\shared\\") { } # We can also apply it across all files to be safe, but just to be sure
    
    $content = Get-Content $f.FullName -Raw
    $original = $content
    
    $content = $content -replace "com\.holaho\.intern\.enums\.", "com.holaho.intern.shared.enums."
    $content = $content -replace "com\.holaho\.intern\.security\.", "com.holaho.intern.shared.security."
    $content = $content -replace "com\.holaho\.intern\.dto\.InternCountStatDto", "com.holaho.intern.shared.dto.InternCountStatDto"
    $content = $content -replace "com\.holaho\.intern\.dto\.StoredFile", "com.holaho.intern.shared.dto.StoredFile"
    
    if (($content -match "extends BaseEntity") -and ($content -notmatch "import com\.holaho\.intern\.shared\.entity\.BaseEntity;")) {
        # Insert import after package declaration
        $content = $content -replace "(package [^;]+;)", "`$1`r`n`r`nimport com.holaho.intern.shared.entity.BaseEntity;"
    }
    
    if ($content -cne $original) {
        Write-Host "Updating $($f.FullName)"
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
    }
}
Write-Host "Done"
