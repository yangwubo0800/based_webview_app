//
//  Tencent.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/2/23.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>
#import<QCloudSDK/QCloudSDK.h>

NS_ASSUME_NONNULL_BEGIN

@interface Tencent : NSObject <QCloudRealTimeRecognizerDelegate>

@property (nonatomic, strong) QCloudRealTimeRecognizer *realTimeRecognizer;

@property (nonatomic, assign) BOOL isRecording;


//获取单实例
+(Tencent *)sharedInstance;

// 启动录音
-(void)startRecord :(NSString *) jsonParam;

// 停止录音
-(void)stopRecord;

// 识别结果
@property (nonatomic, strong) NSString * result;


@end

NS_ASSUME_NONNULL_END
