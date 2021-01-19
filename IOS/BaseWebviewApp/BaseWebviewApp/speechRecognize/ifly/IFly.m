//
//  IFly.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/1/12.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import "IFly.h"
#import "Constant.h"
#import "ISRDataHelper.h"
#import "WebviewController.h"

@implementation IFly


//单例模式
+(IFly *)sharedInstance {
    static IFly  * instance = nil;
    static dispatch_once_t predict;
    dispatch_once(&predict, ^{
        if (instance == nil) {
            instance = [[IFly alloc] init];
        }
    });
    return instance;
}

// 会循环引用调用此方法， 造成dispatch_once 异常
//+(id)allocWithZone:(struct _NSZone *)zone{
//    return [IFly sharedInstance];
//}

-(id)copyWithZone:(NSZone *)zone{
    return [IFly sharedInstance];
}

-(id)mutableCopyWithZone:(NSZone *)zone{
    return [IFly sharedInstance];
}



#pragma mark - Initialization

/**
 initialize recognition conctol and set recognition params
 **/
-(void)initRecognizer:(NSString *) jsonParam
{
    NSLog(@"%s",__func__);
    
    //recognition singleton without view
    if (_iFlySpeechRecognizer == nil) {
        _iFlySpeechRecognizer = [IFlySpeechRecognizer sharedInstance];
    }
    [_iFlySpeechRecognizer setParameter:@"" forKey:[IFlySpeechConstant PARAMS]];
    //set recognition domain
    [_iFlySpeechRecognizer setParameter:@"iat" forKey:[IFlySpeechConstant IFLY_DOMAIN]];
    _iFlySpeechRecognizer.delegate = self;
    if (_iFlySpeechRecognizer != nil) {
        // TODO: 解析json参数，如果为空，则使用默认值，如果不为空，则使用参数传递值
        NSString* language = @"zh_cn";
        NSString* accent = @"mandarin";
        NSString* vadBos = @"4000";
        NSString* vadEos = @"2000";
        NSString* ptt = @"1";
        if (nil != jsonParam) {
            @try {
                //解析json格式参数
                NSError *error;
                NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonParam dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
                NSLog(@"IFlyStartRecord dict=%@", dict);
                NSArray *keys = [dict allKeys];
                
                for(int i=0; i<keys.count; i++){
                    NSString *key = keys[i];
                    if ([@"language" isEqualToString:key]){
                        NSString* lanTemp = [dict valueForKey:key];
                        if (nil != lanTemp) {
                            language = lanTemp;
                        }
                    }
                    
                    if([@"accent" isEqualToString:key]){
                        NSString* accentTemp = [dict valueForKey:key];
                        if (nil != accentTemp) {
                            accent = accentTemp;
                        }
                    }
                    
                    if([@"vadBos" isEqualToString:key]){
                        NSString* vadBosTemp = [dict valueForKey:key];
                        if (nil != vadBosTemp) {
                            vadBos = vadBosTemp;
                        }
                    }
                    
                    if([@"vadEos" isEqualToString:key]){
                        NSString* vadEosTemp = [dict valueForKey:key];
                        if (nil != vadEosTemp) {
                            vadEos = vadEosTemp;
                        }
                    }
                    
                    if([@"ptt" isEqualToString:key]){
                        NSString* pttTemp = [dict valueForKey:key];
                        if (nil != pttTemp) {
                            ptt = pttTemp;
                        }
                    }
                }
            } @catch (NSException *exception) {
                NSLog(@" exception happened");
            } @finally {
                 NSLog(@"finally");
            }
        }
        
        //set timeout of recording
        [_iFlySpeechRecognizer setParameter:IFLY_SPEECH_TIMEOUT forKey:[IFlySpeechConstant SPEECH_TIMEOUT]];
        //set VAD timeout of end of speech(EOS)
        [_iFlySpeechRecognizer setParameter:vadEos forKey:[IFlySpeechConstant VAD_EOS]];
        //set VAD timeout of beginning of speech(BOS)
        [_iFlySpeechRecognizer setParameter:vadBos forKey:[IFlySpeechConstant VAD_BOS]];
        //set network timeout
        [_iFlySpeechRecognizer setParameter:@"20000" forKey:[IFlySpeechConstant NET_TIMEOUT]];
        
        //set sample rate, 16K as a recommended option
        [_iFlySpeechRecognizer setParameter:@"16000" forKey:[IFlySpeechConstant SAMPLE_RATE]];

        //set language
        [_iFlySpeechRecognizer setParameter:language forKey:[IFlySpeechConstant LANGUAGE]];
        //set accent
        [_iFlySpeechRecognizer setParameter:accent forKey:[IFlySpeechConstant ACCENT]];

        //set whether or not to show punctuation in recognition results
        [_iFlySpeechRecognizer setParameter:ptt forKey:[IFlySpeechConstant ASR_PTT]];
        
    }
    
    // 录音文件识别，暂时先不开启
    //Initialize recorder
//    if (_pcmRecorder == nil)
//    {
//        _pcmRecorder = [IFlyPcmRecorder sharedInstance];
//    }
//
//    _pcmRecorder.delegate = self;
//
//    [_pcmRecorder setSample:@"16000"];
//
//    [_pcmRecorder setSaveAudioPath:nil];    //not save the audio file

//    if([[IATConfig sharedInstance].language isEqualToString:@"en_us"]){
//        if([IATConfig sharedInstance].isTranslate){
//            [self translation:NO];
//        }
//    }
//    else{
//        if([IATConfig sharedInstance].isTranslate){
//            [self translation:YES];
//        }
//    }

}


#pragma mark - Button Handling
-(void)startRecord:(NSString *) jsonParam{
    
    NSLog(@"%s[IN]",__func__);
    
    if(_iFlySpeechRecognizer == nil)
    {
        [self initRecognizer:jsonParam];
    }
    
    [_iFlySpeechRecognizer cancel];
    //Set microphone as audio source
    [_iFlySpeechRecognizer setParameter:IFLY_AUDIO_SOURCE_MIC forKey:@"audio_source"];
    //Set result type
    [_iFlySpeechRecognizer setParameter:@"json" forKey:[IFlySpeechConstant RESULT_TYPE]];
    //Set the audio name of saved recording file while is generated in the local storage path of SDK,by default in library/cache.
    [_iFlySpeechRecognizer setParameter:@"asr.pcm" forKey:[IFlySpeechConstant ASR_AUDIO_PATH]];
    
    [_iFlySpeechRecognizer setDelegate:self];
    
    // 先清空返回结果
    //empty results
    _result = nil;
    BOOL ret = [_iFlySpeechRecognizer startListening];
    
    if (ret) {
        NSLog(@"====启动录音成功");
        
    }else{
        NSLog(@"====启动录音失败ret=%d", ret);
    }
}


/**
 stop recording
 **/
- (void)stopRecord {
    
    NSLog(@"%s",__func__);
    
    // 录音文件暂停，先不需要
//    if(self.isStreamRec && !self.isBeginOfSpeech){
//        NSLog(@"%s,stop recording",__func__);
//        [_pcmRecorder stop];
//    }
    
    [_iFlySpeechRecognizer stopListening];
}



#pragma mark - IFlySpeechRecognizerDelegate

/**
 volume callback,range from 0 to 30.
 **/
- (void) onVolumeChanged: (int)volume
{
    NSString * vol = [NSString stringWithFormat:@"%@：%d", NSLocalizedString(@"T_RecVol", nil),volume];
    //NSLog(@"onVolumeChanged: %@", vol);
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
//    js = [js stringByAppendingString:@"','"];
//    js = [js stringByAppendingString:self.result];
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
//    js = [js stringByAppendingString:@"','"];
//    js = [js stringByAppendingString:self.result];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
    
}


/**
 recognition session completion, which will be invoked no matter whether it exits error.
 error.errorCode =
 0     success
 other fail
 **/
- (void) onCompleted:(IFlySpeechError *) error
{
    NSLog(@"%s",__func__);
    NSString *text ;
    
   if (error.errorCode == 0 ) {
        if (_result.length == 0) {
            text = NSLocalizedString(@"T_ISR_NoRlt", nil);
        }else {
            text = NSLocalizedString(@"T_ISR_Succ", nil);
            //empty results
            _result = nil;
        }
    }else {
        text = [NSString stringWithFormat:@"Error：%d %@", error.errorCode,error.errorDesc];
        NSLog(@"%@",text);
    }
}

/**
 result callback of recognition without view
 results：recognition results
 isLast：whether or not this is the last result
 **/
- (void) onResults:(NSArray *) results isLast:(BOOL)isLast
{
    NSMutableString *resultString = [[NSMutableString alloc] init];
    NSDictionary *dic = results[0];
    
    for (NSString *key in dic) {
        [resultString appendFormat:@"%@",key];
    }
    
    
    NSString * resultFromJson =  nil;
    resultFromJson = [ISRDataHelper stringFromJson:resultString];
    
    // 第一次识别需要赋值，否则会出现null字符串
    if (nil == _result) {
        _result = resultFromJson;
    }else{
        //将解析结果赋值, 拼接起来
        _result = [NSString stringWithFormat:@"%@%@", _result,resultFromJson];
    }

    if (isLast){
        NSLog(@"ISR Results(json)：%@",  self.result);
        // TODO；应该在此处发送给前端，才能保持识别的标点符合的完整性
        WebviewController *wc = [WebviewController shareInstance];
        //修改为使用webview直接发送emit时间给前端
        NSString *emitName = @"speechText";
        NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
        js = [js stringByAppendingString:@"','"];
        js = [js stringByAppendingString:self.result];
        js = [js stringByAppendingString:@"')"];
        NSLog(@" js is :%@", js);
        [[wc webView] evaluateJavaScript:js completionHandler:nil];
    }
    NSLog(@"_result=%@",_result);
    NSLog(@"resultFromJson=%@",resultFromJson);
}




@end
