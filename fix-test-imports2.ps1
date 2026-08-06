$files = Get-ChildItem -Path ".\src\test\java" -Recurse -Filter *.java

foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $original = $content
    
    $content = $content -replace "import com\.holaho\.intern\.entity\.InternProfile;", "import com.holaho.intern.intern.entity.InternProfile;"
    $content = $content -replace "import com\.holaho\.intern\.entity\.Role;", "import com.holaho.intern.user.entity.Role;"
    $content = $content -replace "import com\.holaho\.intern\.entity\.User;", "import com.holaho.intern.user.entity.User;"
    $content = $content -replace "import com\.holaho\.intern\.repository\.InternProfileRepository;", "import com.holaho.intern.intern.repository.InternProfileRepository;"
    $content = $content -replace "import com\.holaho\.intern\.repository\.RoleRepository;", "import com.holaho.intern.user.repository.RoleRepository;"
    $content = $content -replace "import com\.holaho\.intern\.repository\.UserRepository;", "import com.holaho.intern.user.repository.UserRepository;"
    $content = $content -replace "import com\.holaho\.intern\.entity\.Task;", "import com.holaho.intern.task.entity.Task;"
    $content = $content -replace "import com\.holaho\.intern\.repository\.TaskRepository;", "import com.holaho.intern.task.repository.TaskRepository;"

    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.AuditLogServiceImpl;", "import com.holaho.intern.service.AuditLogServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.AuthServiceImpl;", "import com.holaho.intern.auth.service.AuthServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.InternProfileServiceImpl;", "import com.holaho.intern.intern.service.InternProfileServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.TaskServiceImpl;", "import com.holaho.intern.task.service.TaskServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.WeeklyReportServiceImpl;", "import com.holaho.intern.service.WeeklyReportServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.impl\.SystemConfigServiceImpl;", "import com.holaho.intern.service.SystemConfigServiceImpl;"
    $content = $content -replace "import com\.holaho\.intern\.service\.InternProfileService;", "import com.holaho.intern.intern.service.InternProfileService;"
        
    if ($content -cne $original) {
        Write-Host "Updating $($f.FullName)"
        $utf8NoBom = New-Object System.Text.UTF8Encoding $false
        [System.IO.File]::WriteAllText($f.FullName, $content, $utf8NoBom)
    }
}
Write-Host "Done fast fix test imports"
