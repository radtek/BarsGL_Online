set TAG=%1
set BRANCH=%2

echo Creating local branch %BRANCH% ...
git branch %BRANCH% %TAG%

echo Pushing local branch %BRANCH% to remote repository ...
git push --progress --porcelain origin refs/heads/%BRANCH%:%BRANCH% --set-upstream