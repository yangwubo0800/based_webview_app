//
//  Baidu.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/1/26.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import "Baidu.h"
#import "WebviewController.h"
#import "Constant.h"


@implementation Baidu

//单例模式
+(Baidu *) sharedInstance{
    static Baidu * instance = nil;
    static dispatch_once_t predict;
    dispatch_once(&predict, ^{
        if (nil == instance) {
            instance = [[Baidu alloc]init];
        }
    });
    
    return instance;
}


-(id)copyWithZone:(NSZone *)zone{
    return [Baidu sharedInstance];
}

-(id)mutableCopyWithZone:(NSZone *)zone{
    return [Baidu sharedInstance];
}



//开始语音识别
-(void)startRecord :(NSString *) jsonParam{
    NSLog(@"=====startRecord jsonParam=%@", jsonParam);
    // 创建对象
    if (nil == self.asrEventManager ) {
        self.asrEventManager = [BDSEventManager createEventManagerWithName:BDS_ASR_NAME];
    }
    // 清空返回结果
    self.result = @"";
    // TODO: 根据传入参数，来配置pid 和 使用短语音识别还是长语音识别
    NSString *pid = @"15372";
    NSString *vadEndTimeout = @"0";
    if (nil != jsonParam) {
        @try {
            //解析json格式参数
            NSError *error;
            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonParam dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
            NSLog(@"startRecord dict=%@", dict);
            NSArray *keys = [dict allKeys];
            
            for(int i=0; i<keys.count; i++){
                NSString *key = keys[i];
                if ([@"pid" isEqualToString:key]){
                    pid = [dict valueForKey:key];
                }
                
                if([@"vad.endpoint-timeout" isEqualToString:key]){
                    vadEndTimeout = [dict valueForKey:key];
                }
            }

        } @catch (NSException *exception) {
            NSLog(@" exception happened");
        } @finally {
            NSLog(@"finally");
        }
    }else{
        NSLog(@"json param is nil, 默认使用中文短语音识别");
    }
   
    // 设置参数
    [self configVoiceRecognitionClient:pid];
    // 和安卓平台统一致，使用json参数来决定使用长短语音识别
    if ([@"0" isEqualToString:vadEndTimeout]) {
        [self longSpeechRecognition];
    }else{
        [self shortSpeechRecognition];
    }
}

// 停止语音识别
-(void)stopRecord{
    NSLog(@"=====stopRecord");
    if (self.asrEventManager) {
        [self.asrEventManager sendCommand:BDS_ASR_CMD_STOP];
    }
}


- (void)shortSpeechRecognition {

    self.longSpeechFlag = NO;
    [self.asrEventManager setParameter:@(NO) forKey:BDS_ASR_ENABLE_LONG_SPEECH];
    [self.asrEventManager setParameter:@(NO) forKey:BDS_ASR_NEED_CACHE_AUDIO];
    [self.asrEventManager setParameter:@"" forKey:BDS_ASR_OFFLINE_ENGINE_TRIGGERED_WAKEUP_WORD];
    [self voiceRecogButtonHelper];
}



- (void)longSpeechRecognition
{
//    [self cleanLogUI];
    self.longSpeechFlag = YES;
    [self.asrEventManager setParameter:@(NO) forKey:BDS_ASR_NEED_CACHE_AUDIO];
    [self.asrEventManager setParameter:@"" forKey:BDS_ASR_OFFLINE_ENGINE_TRIGGERED_WAKEUP_WORD];
    [self.asrEventManager setParameter:@(YES) forKey:BDS_ASR_ENABLE_LONG_SPEECH];
    // 长语音请务必开启本地VAD
    [self.asrEventManager setParameter:@(YES) forKey:BDS_ASR_ENABLE_LOCAL_VAD];
    [self voiceRecogButtonHelper];
}


- (void)onStartWorking
{
    NSLog(@"=====onStartWorking");
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechBegin";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"')"];
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
    
}

-(void)showSpeechText{
    // TODO: 发送结束事件给前端， 短语音识别正常结束，出错
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechText";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"','"];
    js = [js stringByAppendingString:self.result];
    js = [js stringByAppendingString:@"')"];
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
}

- (void)onEnd
{
    self.longSpeechFlag = NO;
    // TODO: 此处是本地识别最终结束的回调，需要通知给前端
    NSLog(@"=====onEnd");
    WebviewController *wc = [WebviewController shareInstance];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"speechEnd";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"')"];
    [[wc webView] evaluateJavaScript:js completionHandler:nil];

}

- (void)voiceRecogButtonHelper
{
    //    [self configFileHandler];
    [self.asrEventManager setDelegate:self];
    [self.asrEventManager setParameter:nil forKey:BDS_ASR_AUDIO_FILE_PATH];
    [self.asrEventManager setParameter:nil forKey:BDS_ASR_AUDIO_INPUT_STREAM];
    [self.asrEventManager sendCommand:BDS_ASR_CMD_START];
//    [self onInitializing];
}



#pragma mark - MVoiceRecognitionClientDelegate

- (void)VoiceRecognitionClientWorkStatus:(int)workStatus obj:(id)aObj {
    switch (workStatus) {
        case EVoiceRecognitionClientWorkStatusNewRecordData: {
            //[self.fileHandler writeData:(NSData *)aObj];
            break;
        }
            
        case EVoiceRecognitionClientWorkStatusStartWorkIng: {
            NSDictionary *logDic = [self parseLogToDic:aObj];
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: start vr, log: %@\n", logDic]];
            [self onStartWorking];
            break;
        }
        case EVoiceRecognitionClientWorkStatusStart: {
            [self printLogTextView:@"CALLBACK: detect voice start point.\n"];
            NSLog(@"=====EVoiceRecognitionClientWorkStatusStart");
            break;
        }
        case EVoiceRecognitionClientWorkStatusEnd: {
            [self printLogTextView:@"CALLBACK: detect voice end point.\n"];
            NSLog(@"=====EVoiceRecognitionClientWorkStatusEnd");
            break;
        }
        case EVoiceRecognitionClientWorkStatusFlushData: {
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: partial result - %@.\n\n", [self getDescriptionForDic:aObj]]];
            break;
        }
        case EVoiceRecognitionClientWorkStatusFinish: {
            NSLog(@"=====EVoiceRecognitionClientWorkStatusFinish");
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: final result - %@.\n\n", [self getDescriptionForDic:aObj]]];
//            if (aObj) {
//                self.resultTextView.text = [self getDescriptionForDic:aObj];
//            }
            // 将识别结果赋值到变量中
            if (aObj) {
                [self getFinalSpeechRecognizeResult:aObj];
            }
            if (!self.longSpeechFlag) {
                // 在短语音识别情况下，结束此处识别会话
                [self onEnd];
                [self showSpeechText];
            }else{
                // 长语音情况下，为了一次返回结果，在最终停止EVoiceRecognitionClientWorkStatusLongSpeechEnd时返回
                //[self showSpeechText];
            }
            break;
        }
        case EVoiceRecognitionClientWorkStatusMeterLevel: {
            break;
        }
        case EVoiceRecognitionClientWorkStatusCancel: {
            [self printLogTextView:@"CALLBACK: user press cancel.\n"];
            [self onEnd];
            break;
        }
        case EVoiceRecognitionClientWorkStatusError: {
            NSLog(@"=====EVoiceRecognitionClientWorkStatusError");
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: encount error - %@.\n", (NSError *)aObj]];
            // TODO: 将当次最终的识别结果返回给前端
            // 将返回结果清空，并且返回以及结束
            self.result=@"";
            [self showSpeechText];
            // 长语音中在没有语音输入时会有多次出错回调
            if (!self.longSpeechFlag) {
                [self onEnd];
            }
            break;
        }
        case EVoiceRecognitionClientWorkStatusLoaded: {
            [self printLogTextView:@"CALLBACK: offline engine loaded.\n"];
            break;
        }
        case EVoiceRecognitionClientWorkStatusUnLoaded: {
            [self printLogTextView:@"CALLBACK: offline engine unLoaded.\n"];
            break;
        }
        case EVoiceRecognitionClientWorkStatusChunkThirdData: {
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: Chunk 3-party data length: %lu\n", (unsigned long)[(NSData *)aObj length]]];
            break;
        }
        case EVoiceRecognitionClientWorkStatusChunkNlu: {
            NSString *nlu = [[NSString alloc] initWithData:(NSData *)aObj encoding:NSUTF8StringEncoding];
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: Chunk NLU data: %@\n", nlu]];
            NSLog(@"%@", nlu);
            break;
        }
        case EVoiceRecognitionClientWorkStatusChunkEnd: {
            NSLog(@"=====EVoiceRecognitionClientWorkStatusChunkEnd");
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK: Chunk end, sn: %@.\n", aObj]];
//            if (!self.longSpeechFlag) {
//                [self onEnd];
//            }
            // 此事件在长语音识别时会多次回调，因此返回文本只能短语音情况下是如此
//            if (!self.longSpeechFlag) {
//                [self showSpeechText];
//            }
            break;
        }
        case EVoiceRecognitionClientWorkStatusFeedback: {
            NSDictionary *logDic = [self parseLogToDic:aObj];
            [self printLogTextView:[NSString stringWithFormat:@"CALLBACK Feedback: %@\n", logDic]];
            break;
        }
        case EVoiceRecognitionClientWorkStatusRecorderEnd: {
            [self printLogTextView:@"CALLBACK: recorder closed.\n"];
            break;
        }
        case EVoiceRecognitionClientWorkStatusLongSpeechEnd: {
            NSLog(@"=====EVoiceRecognitionClientWorkStatusLongSpeechEnd");
            // 调用接口结束长语音识别之后回调
            [self printLogTextView:@"CALLBACK: Long Speech end.\n"];
            [self onEnd];
            // 长语音结束后返回最终识别结果
            [self showSpeechText];
            break;
        }
        default:
            break;
    }
}



- (void)printLogTextView:(NSString *)logString
{
//    self.logTextView.text = [logString stringByAppendingString:_logTextView.text];
//    [self.logTextView scrollRangeToVisible:NSMakeRange(0, 0)];
    NSLog(@"####printLogTextView %@", logString);
}

- (NSDictionary *)parseLogToDic:(NSString *)logString
{
    NSArray *tmp = NULL;
    NSMutableDictionary *logDic = [[NSMutableDictionary alloc] initWithCapacity:3];
    NSArray *items = [logString componentsSeparatedByString:@"&"];
    for (NSString *item in items) {
        tmp = [item componentsSeparatedByString:@"="];
        if (tmp.count == 2) {
            [logDic setObject:tmp.lastObject forKey:tmp.firstObject];
        }
    }
    return logDic;
}



#pragma mark - Private: Configuration

- (void)configVoiceRecognitionClient:(NSString *) pid {
    //设置DEBUG_LOG的级别
    [self.asrEventManager setParameter:@(EVRDebugLogLevelOff) forKey:BDS_ASR_DEBUG_LOG_LEVEL];
    //配置API_KEY 和 SECRET_KEY 和 APP_ID
    [self.asrEventManager setParameter:@[BAIDU_SPEECH_APPKEY, BAIDU_SPEECH_SECRETKEY] forKey:BDS_ASR_API_SECRET_KEYS];
    [self.asrEventManager setParameter:BAIDU_SPEECH_APPID forKey:BDS_ASR_OFFLINE_APP_CODE];
    [self.asrEventManager setParameter:@"1537" forKey:BDS_ASR_PRODUCT_ID];
    
    //配置端点检测（二选一）
    [self configModelVAD];
    //    [self configDNNMFE];
    
    //    [self.asrEventManager setParameter:@"15361" forKey:BDS_ASR_PRODUCT_ID];
    // ---- 语义与标点 -----
    //    [self enableNLU];
    //    [self enablePunctuation];
    // ------------------------
    
    //---- 语音自训练平台 ----
//        [self configSmartAsr];
}




- (void)configModelVAD {
    NSString *modelVAD_filepath = [[NSBundle mainBundle] pathForResource:@"bds_easr_basic_model" ofType:@"dat"];
    [self.asrEventManager setParameter:modelVAD_filepath forKey:BDS_ASR_MODEL_VAD_DAT_FILE];
    [self.asrEventManager setParameter:@(YES) forKey:BDS_ASR_ENABLE_MODEL_VAD];
}

- (void)configDNNMFE {
    NSString *mfe_dnn_filepath = [[NSBundle mainBundle] pathForResource:@"bds_easr_mfe_dnn" ofType:@"dat"];
    [self.asrEventManager setParameter:mfe_dnn_filepath forKey:BDS_ASR_MFE_DNN_DAT_FILE];
    NSString *cmvn_dnn_filepath = [[NSBundle mainBundle] pathForResource:@"bds_easr_mfe_cmvn" ofType:@"dat"];
    [self.asrEventManager setParameter:cmvn_dnn_filepath forKey:BDS_ASR_MFE_CMVN_DAT_FILE];
    // 自定义静音时长
    //    [self.asrEventManager setParameter:@(501) forKey:BDS_ASR_MFE_MAX_SPEECH_PAUSE];
    //    [self.asrEventManager setParameter:@(500) forKey:BDS_ASR_MFE_MAX_WAIT_DURATION];
}




#pragma mark - Private: File


// 显示原始json结果
- (NSString *)getDescriptionForDic:(NSDictionary *)dic {
    if (dic) {
        return [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:dic
                                                                              options:NSJSONWritingPrettyPrinted
                                                                                error:nil] encoding:NSUTF8StringEncoding];
    }
    
    return nil;
}

// 获取最终识别结果
-(void)getFinalSpeechRecognizeResult:(NSDictionary  *)dic{
    if (dic) {
        // TODO: 增加解析最终识别结果
        NSArray *keys = [dic allKeys];
        NSArray *results;
        NSString * finalResult = [[NSString alloc] init];
        for(int i=0; i<keys.count; i++){
            NSString *key = keys[i];
            if ([@"results_recognition" isEqualToString:key]){
                results = [dic valueForKey:key];
                if (results) {
                    for(int index=0; index < results.count; index++){
                        NSString *result = results[index];
                        NSLog(@"百度语音识别结果字典数组result=%@", result);
                        finalResult = [finalResult stringByAppendingString:result];
                    }
                }else{
                    NSLog(@"百度语音识别字典返回数据中没有 results_recognition 信息");
                }
            }
        }
        NSLog(@"百度语音识别此次识别到的最终结果 finalResult=%@", finalResult);
        // 对于长语音，每次的结果需要拼接返回
        self.result = [self.result stringByAppendingString:finalResult];
    }else{
        NSLog(@"百度语音识别 dic is nil");
    }
}


@end

