//
//  Constant.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2020/9/4.
//  Copyright © 2020 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

// 此文件以后专门用来定义常量，便于修改查找管理。


//JPush param
#define JpushAppKey   @"xxxxxxx"
#define JpushChannel   @"Publish channel"

//baidu push param
#define BaiduPushApiKey  @"xxxxxx"


// GTSDK 配置信息
#define kGtAppId @"xxxxxx"
#define kGtAppKey @"xxxxx"
#define kGtAppSecret @"xxxxxx"

// 科大讯飞语音识别应用ID
#define IFLY_APPID_VALUE           @"xxxxxx"
//录音超时，单位为毫秒 ，最多一分钟
#define IFLY_SPEECH_TIMEOUT        @"60000"

//百度语音识别key配置
#define BAIDU_SPEECH_APPID   @"xxxxxx"
#define BAIDU_SPEECH_APPKEY   @"xxxxxx"
#define BAIDU_SPEECH_SECRETKEY   @"xxxxxx"

//腾讯语音识别
#define kQDAppId     @"xxxxxx"
#define kQDSecretId  @"xxxxxx"
#define kQDSecretKey @"xxxxxx"

//推送消息跳转页面key定义
static NSString *const PUSH_MESSAGE_JUMP_URL_KEY = @"historyAlarmUrl";

@interface Constant : NSObject


@end

NS_ASSUME_NONNULL_END
