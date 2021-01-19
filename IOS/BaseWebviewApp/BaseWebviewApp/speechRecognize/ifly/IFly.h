//
//  IFly.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/1/12.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "IFlyMSC/IFlyMSC.h"

NS_ASSUME_NONNULL_BEGIN

@interface IFly : NSObject <IFlySpeechRecognizerDelegate>

//获取单实例
+(IFly *)sharedInstance;

// 启动录音
-(void)startRecord:(NSString *) jsonParam;


// 停止录音
- (void)stopRecord;

// 识别器
@property (nonatomic, strong) IFlySpeechRecognizer *iFlySpeechRecognizer;
// 识别结果
@property (nonatomic, strong) NSString * result;


//@property (nonatomic,strong) IFlyPcmRecorder *pcmRecorder;//PCM Recorder to be used to demonstrate Audio Stream Recognition.
//@property (nonatomic,assign) BOOL isStreamRec;//Whether or not it is Audio Stream function
//@property (nonatomic,assign) BOOL isBeginOfSpeech;//Whether or not SDK has invoke the delegate methods of beginOfSpeech.


@end

NS_ASSUME_NONNULL_END
