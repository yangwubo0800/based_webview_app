#!/bin/sh
echo "This is a MAC, let start building..."

cd /Users/hongboni/Svn/hzinfo3000platform/hzinfo-app/IOS/BaseWebviewApp

xcodebuild  -project BaseWebviewApp.xcodeproj/  clean

echo "start to sleep..."
sleep 2

xcodebuild -scheme BaseWebviewApp -archivePath build/BaseWebviewApp.xcarchive archive

echo "start to sleep..."
sleep 5

xcodebuild -exportArchive -archivePath build/BaseWebviewApp.xcarchive -exportPath build/BaseWebviewApp.ipa -exportOptionsPlist ExportOptions.plist

echo "start to sleep..."
sleep 5





