#!/bin/bash

# TgoRTC SDK 本地发布脚本
# 将 SDK 发布到项目根目录的 repo 文件夹

set -e

echo "🚀 开始发布 TgoRTC SDK 到本地仓库..."

# 清理之前的构建
./gradlew clean

# 发布到项目根目录 repo 本地仓库
./gradlew :tgortc:publishReleasePublicationToLocalRepository

echo ""
echo "✅ 发布成功！"
echo ""
echo "📦 库已发布到: $(pwd)/repo"
echo ""
echo "📝 在其他项目中使用此库，请在 settings.gradle.kts 添加:"
echo ""
echo '   dependencyResolutionManagement {'
echo '       repositories {'
echo '           maven {'
echo '               url = uri("/Users/songlun/Desktop/workspace/androidworkspace/TgoRTCSDK/repo")'
echo '           }'
echo '       }'
echo '   }'
echo ""
echo "📝 在 build.gradle.kts 中添加依赖:"
echo ""
echo '   implementation("com.tgo.rtc:tgortc:1.0.1-local")'
echo ""
