//
//  Baidu.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/1/26.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "BDSASRDefines.h"
#import "BDSASRParameters.h"
#import "BDSEventManager.h"

NS_ASSUME_NONNULL_BEGIN

@interface Baidu : NSObject<BDSClientASRDelegate>

@property (strong, nonatomic) BDSEventManager *asrEventManager;

@property(nonatomic, assign) BOOL longSpeechFlag;


//获取单实例
+(Baidu *)sharedInstance;

// 启动录音
-(void)startRecord :(NSString *) jsonParam;

// 停止录音
-(void)stopRecord;

// 识别结果
@property (nonatomic, strong) NSString * result;

@end

NS_ASSUME_NONNULL_END
