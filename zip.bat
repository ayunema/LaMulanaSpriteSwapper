rmdir target\LaMulanaSpriteSwapper /S /Q
del target\SpriteSwapper.zip
mkdir target\LaMulanaSpriteSwapper
#xcopy "src\main\resources\sprites\" "target\LaMulanaSpriteSwapper\sprites\" /E
#del "LaMulanaSpriteSwapper\sprites\*.psd" /S /Q
xcopy /I target\*Swapper*.jar "target\LaMulanaSpriteSwapper\*"
cd target
7z a SpriteSwapper.zip LaMulanaSpriteSwapper/*
#rmdir LaMulanaSpriteSwapper /S /Q
start explorer .