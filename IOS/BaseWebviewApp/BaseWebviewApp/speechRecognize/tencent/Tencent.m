//
//  Tencent.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/2/23.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import "Tencent.h"
#import "WebviewController.h"
#import "Constant.h"

#define kQDProjectId @""
#define kQDSn        @""
#define kQDDeviceId  @""


@implementation Tencent


//单例模式
+(Tencent *) sharedInstance{
    static Tencent * instance = nil;
    static dispatch_once_t predict;
    dispatch_once(&predict, ^{
        if (nil == instance) {
            instance = [[Tencent alloc]init];
        }
    });
    
    return instance;
}


-(id)copyWithZone:(NSZone *)zone{
    return [Tencent sharedInstance];
}

-(id)mutableCopyWithZone:(NSZone *)zone{
    return [Tencent sharedInstance];
}



//开始语音识别
-(void)startRecord :(NSString *) jsonParam{
    
    // TODO: 解析json参数，如果为空，则使用默认值，如果不为空，则使用参数传递值
    NSString* language = @"16k_zh";
    NSString* vad = @"0";
    NSString* vadTimeoout = @"4000";
    NSString* punc = @"0";
    BOOL needVad = NO;
    NSInteger vadTimeoutValue = 5;
    NSInteger puncValue = 0;

    if (nil != jsonParam) {
        @try {
            //解析json格式参数
            NSError *error;
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonParam dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
            NSArray *keys = [dict allKeys];
            
            for(int i=0; i<keys.count; i++){
                NSString *key = keys[i];
                if ([@"language" isEqualToString:key]){
                    NSString* lanTemp = [dict valueForKey:key];
                    if (nil != lanTemp) {
                        language = lanTemp;
                    }
                }
                
                if([@"vad" isEqualToString:key]){
                    NSString* vadTemp = [dict valueForKey:key];
                    if (nil != vadTemp) {
                        vad = vadTemp;
                    }
                }
                
                if([@"vadTimeoout" isEqualToString:key]){
                    NSString* vadTimeooutTemp = [dict valueForKey:key];
                    if (nil != vadTimeooutTemp) {
                        vadTimeoout = vadTimeooutTemp;
                    }
                }
                
                if([@"punc" isEqualToString:key]){
                    NSString* puncTemp = [dict valueForKey:key];
                    if (nil != puncTemp) {
                        punc = puncTemp;
                    }
                }
            }
            // 转换是否要开启静音检测
            if ([vad isEqualToString:@"1"]) {
                needVad = YES;
            }else if ([vad isEqualToString:@"0"]){
                needVad = NO;
            }else{
                NSLog(@" need vad default value is %d", needVad);
            }
            vadTimeoutValue = [vadTimeoout integerValue];
            puncValue = [punc integerValue];
            
        } @catch (NSException *exception) {
            NSLog(@" exception happened");
        } @finally {
             NSLog(@"finally");
        }
    }
    
    if (!_realTimeRecognizer) {
        NSLog(@"=====tencent config language=%@ vad=%@ vadTimeout=%@ punc=%@", language, vad, vadTimeoout, punc);
        //1.创建QCloudConfig实例
        QCloudConfig *config = [[QCloudConfig alloc] initWithAppId:kQDAppId secretId:kQDSecretId secretKey:kQDSecretKey projectId:[kQDProjectId integerValue]];
        config.sliceTime = 600;                             //语音分片时长600ms
        config.enableDetectVolume = YES; //是否检测音量
        //config.endRecognizeWhenDetectSilence = YES; //是否检测到静音停止识别
        config.requestTimeout = 10;
        config.enableDebugLog = YES;
//        config.enableReportCrash = YES;
        // 设置传递的参数
        config.engineType = language;
        config.filterPunc = puncValue;
        config.endRecognizeWhenDetectSilence = needVad;
        config.silenceDetectDuration = vadTimeoutValue;
        //@important: 使用外部数据源传入语音数据，自定义data source需要实现QCloudAudioDataSource协议
//        QCloudDemoAudioDataSource *dataSource = [[QCloudDemoAudioDataSource alloc] init];
//        _realTimeRecognizer = [[QCloudRealTimeRecognizer alloc] initWithConfig:config dataSource:dataSource];
        
        //2.创建QCloudRealTimeRecognizer实例
        NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
        NSInteger authorize = [defaults integerForKey:@"authorize"];
        if (authorize) {
            _realTimeRecognizer = [[QCloudRealTimeRecognizer alloc] initWithConfig:config dataSource:nil sn:kQDSn deviceId:kQDDeviceId];
        } else {
            _realTimeRecognizer = [[QCloudRealTimeRecognizer alloc] initWithConfig:config];
        }
        
        //3.设置delegate
        _realTimeRecognizer.delegate = self;

    }
    [self startRecognizeIfNeed];

}


// 停止语音识别
-(void)stopRecord{
    if (_realTimeRecognizer) {
        [_realTimeRecognizer stop];
    }
}



- (void)startRecognizeIfNeed
{
    //    [self startWithRecorder];
    //[self updateVolume:NSIntegerMax];
    if (_isRecording) {
        _isRecording = NO;
        [_realTimeRecognizer stop];
    }
    else {
        [_realTimeRecognizer start];
    }
}


- (void)stopRecognizeIfNeed
{
    if (_isRecording) {
        _isRecording = NO;
        [_realTimeRecognizer stop];
    }
}




/**
 Beginning Of Speech
 **/
- (void) onBeginOfSpeech
{
    NSLog(@"onBeginOfSpeech");
    
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechBegin";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
}

/**
 End Of Speech
 **/
- (void) onEndOfSpeech
{
    NSLog(@"#####onEndOfSpeech");
    
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechEnd";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
}


-(void)showSpeechText: (NSString*)result{
    // TODO: 发送结束事件给前端， 短语音识别正常结束，出错
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechText";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"','"];
    js = [js stringByAppendingString:result];
    js = [js stringByAppendingString:@"')"];
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
}




#pragma mark - QCloudRealTimeRecognizerDelegate
- (void)realTimeRecognizerOnSliceRecognize:(QCloudRealTimeRecognizer *)recognizer
                                  response:(QCloudRealTimeResponse *)response
{
    if (QCloudRealTimeResponseCodeOk == response.code) {
        //self.recognizedTextView.text = response.recognizedText;
    }
    NSLog(@"realTimeRecognizerOnSliceRecognize response %@", [response debugDescription]);
}

- (void)realTimeRecognizerDidStartRecord:(QCloudRealTimeRecognizer *)recorder error:(NSError *)error
{
    NSLog(@"realTimeRecognizerDidStartRecord error %@", error);
    if (!error) {
        _isRecording = YES;
//        [self startRecognize:YES];
//        [self startAnimation];
//        [self updateButtonTitle];
    }
    [self onBeginOfSpeech];
}

- (void)realTimeRecognizerDidStopRecord:(QCloudRealTimeRecognizer *)recorder
{
    NSLog(@"realTimeRecognizerDidStopRecord");
    _isRecording = NO;
//    [self stopAnimation];
//    [self updateButtonTitle];
    [self onEndOfSpeech];
}

- (void)realTimeRecognizerDidUpdateVolume:(QCloudRealTimeRecognizer *)recognizer volume:(float)volume
{
    NSLog(@"realTimeRecognizerDidUpdateVolume volume:%lf", volume);
//    _volume = volume;
//    [self updateVolume:volume];
}
/*
 * 检测到flow的开始
 */
- (void)realTimeRecognizerOnFlowStart:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq;
{
    NSLog(@"realTimeRecognizerFlowStart:%@ seq:%ld", voiceId, seq);
}
/*
 * 检测flow的结束
 */
- (void)realTimeRecognizerOnFlowEnd:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq;
{
    NSLog(@"realTimeRecognizerFlowEnd:%@ seq:%ld", voiceId, seq);
}

- (void)realTimeRecognizerOnFlowRecognizeStart:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq
{
    NSLog(@"realTimeRecognizerOnFlowRecognizeStart:%@ seq:%ld", voiceId, seq);
}
/**
 * 检测到flow的结束识别
 * @param voiceId flow对应的voiceId
 * @param seq flow的序列号
 */
- (void)realTimeRecognizerOnFlowRecognizeEnd:(QCloudRealTimeRecognizer *)recognizer voiceId:(NSString *)voiceId seq:(NSInteger)seq
{
    NSLog(@"realTimeRecognizerOnFlowRecognizeEnd:%@ seq:%ld", voiceId, seq);
}

- (void)realTimeRecognizerOnSegmentSuccessRecognize:(QCloudRealTimeRecognizer *)recognizer response:(QCloudRealTimeResponse *)response
{
    QCloudRealTimeResultResponse *currentResult = [response.resultList firstObject];
    NSLog(@"realTimeRecognizerOnSegmentSuccessRecognize:%@ index:%ld", currentResult.voiceTextStr, currentResult.index);
}

- (void)realTimeRecognizerDidFinish:(QCloudRealTimeRecognizer *)recorder result:(NSString *)result
{
    NSLog(@"realTimeRecognizerDidFinish:%@", result);
    [self showSpeechText:result];
}

- (void)realTimeRecognizerDidError:(QCloudRealTimeRecognizer *)recorder error:(NSError *)error
{
    NSLog(@"realTimeRecognizerDidError:%@", error);
}




@end
