//
//  JSInterface.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/10/9.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "JSInterface.h"
#import "../LZKeychain.h"
#import "WebviewController.h"
#import "../Utils/JPushUtils.h"
#import "../CameraController.h"
#import "../Utils/StringUtils.h"
#import "../LocationController.h"
#import "../Utils/checkNetwork.h"
#import "RealHtmlController.h"
#import "../Utils/GeneralUtils.h"

@implementation JSInterface

//(id) handler:(id) msg
//参数可以是任何类型, 但是返回值类型不能为 void。 如果不需要参数，也必须声明，声明后不使用就行

// 获取唯一标识
- (NSString *) getDeviceId:(NSString *) msg{
    NSString *deviceId = [LZKeychain getDeviceIDInKeychain];
    NSLog(@"getDeviceId deviceId is %@", deviceId);
    return  deviceId;
}


// 直播
- (NSString *) ijkLivePlay:(NSString *) playPath{
    WebviewController *wc = [WebviewController shareInstance];
    [wc ijkLivePlay:playPath];
    return  @"ijkLivePlay started";
}


// 设置极光推送tag
- (NSString *) setJPushTag:(NSString *) tags{
    [JPushUtils setJPushTags:tags];
    return @"setJPushTag started";
}


// 拍照
- (NSString *) takePhoto:(NSString *) msg{
    WebviewController *wc = [WebviewController shareInstance];
    [[CameraController shareInstance] nativeTakePhoto:wc];
    return @"takePhoto started";
}


// 录像
- (NSString *) recordVideo:(NSString *) msg{
    WebviewController *wc = [WebviewController shareInstance];
    [[CameraController shareInstance] nativeRecordVideo:wc];
    return @"recordVideo started";
}


// 扫码
- (NSString *) scanQRCode:(NSString *) callerName{
    WebviewController *wc = [WebviewController shareInstance];
    [[CameraController shareInstance] scanQRCode:wc];
    return @"scanQRCode started";
}

// 定位
- (NSString *) locate:(NSString *) callerName{
    WebviewController *wc = [WebviewController shareInstance];
    [[LocationController shareInstance] locate:wc];
    return @"locate started";
}


// 视频播放
- (NSString *) videoPlay:(NSString *) playPath{
    WebviewController *wc = [WebviewController shareInstance];
    [wc ijkVideoPlay:playPath];
    return @"videoPlay started";
}


// 拨打电话
- (NSString *) dailNumber:(NSString *) number{
    NSLog(@"number is %@", number);
    NSMutableString *str=[[NSMutableString alloc]initWithFormat:@"tel:%@",number];
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:str]];
    return @"dailNumber started";
}


// 设置key value 键值对数据
- (NSString *) setValueByKey:(NSString*) jsonStr{
    NSLog(@"setValueByKey jsonStr=%@", jsonStr);
    NSError *error;
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonStr dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
    //NSDictionary *dict = jsonStr;
    NSLog(@"setValueByKey dict=%@", dict);
    NSArray *keys = [dict allKeys];
    for(int i=0; i<keys.count; i++){
        NSString *key = keys[i];
        NSString *value = [dict valueForKey:key];
        NSLog(@"key is %@, value is %@", key, value);
        [[NSUserDefaults standardUserDefaults] setObject:value  forKey:key];
    }
    [[NSUserDefaults standardUserDefaults] synchronize];
    return @"setValueByKey started";
}

// 获取value
- (NSString *) getValueByKey:(NSString*) key{
    NSString *value = [[NSUserDefaults standardUserDefaults] objectForKey:key];
    return value;
}


// 检测网络连接是否可用
- (NSString *) checkNetwork:(NSString*) msg{
    BOOL networkAvailable = [checkNetwork checkNetworkCanUse];
    return (networkAvailable == YES) ? @"YES": @"NO";
}


// 启动横屏显示实时画面
- (NSString *) startRealHtml:(NSString*) htmlPath{
    NSString *path = htmlPath;
    //转义字符或字符串中含有中文， 都可能导致url=nil,需要处理
    NSString * urlStr = [path stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    NSURL *url = [NSURL URLWithString:urlStr];
    RealHtmlController *vc = [[RealHtmlController alloc] initWithUrl:url];
    UIBarButtonItem *backBtn = [[UIBarButtonItem alloc] init];
    //默认为英文back，此处修改返回标题为中文
    backBtn.title = @"返回";
    WebviewController *wc = [WebviewController shareInstance];
    wc.navigationItem.backBarButtonItem = backBtn;
    //使用push方式来显示横屏界面，其有导航栏，再其返回时会将屏幕设置回竖屏
    [wc.navigationController pushViewController:vc animated:YES];
    
    return @"startRealHtml started";
}


// 获取app基本信息
- (NSString *) getAppInfo:(NSString*) msg{
    
    NSDictionary *infoDictionary = [[NSBundle mainBundle] infoDictionary];
    NSString *name = [infoDictionary objectForKey:@"CFBundleName"];
    NSString *version = [infoDictionary objectForKey:@"CFBundleShortVersionString"];
    NSString *build = [infoDictionary objectForKey:@"CFBundleVersion"];
    NSLog(@"name is %@, version is %@, build is %@", name, version, build);

    NSDictionary *dict = @{@"name":name,
                           @"version":version,
                           @"build":build
                           };
    NSString *appInfo = [StringUtils UIUtilsFomateJsonWithDictionary:dict];
    return appInfo;
}

// 获取缓存大小
- (NSString *) getCacheSize:(NSString*) msg{

    NSString *cacheSize = [GeneralUtils getCacheSize];
    NSLog(@" get cache size is %@", cacheSize);
    return cacheSize;
}


// 清除缓存
- (NSString *) clearCache:(NSString*) msg{
    
    [GeneralUtils clearCache];
    NSLog(@" clearCache");
    return @"clearCache started";
}

// 测试调用js 触发前端监听事件
- (NSString *) testEmit:(NSString *) callerName{
    WebviewController *wc = [WebviewController shareInstance];
    NSString *emitName = @"location";
    //NSString *locationJson = @"empty";
    NSString *locationJson = @"{\"address\":\"中国湖南省长沙市岳麓区佳园路\",\"latitude\":\"28.229733\",\"longitude\":\"112.864245\",\"name\":\"test1\"}";
    NSString *js = [@"Android.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"','"];
    js = [js stringByAppendingString:locationJson];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    
    NSString *testJs = @"alert";
    testJs = [testJs stringByAppendingString:@"('"];
    testJs = [testJs stringByAppendingString:@"hello"];
    testJs = [testJs stringByAppendingString:@"')"];
    NSLog(@" testJs is :%@", testJs);
    
    NSString *test2 = @"system.model()";
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
    
    NSDictionary *dict = @{@"callerName":@"scanCallerNamevalue", @"scanResult":@"scanResultvalue"};
    NSLog(@"scan resut is %@",dict);
    NSString *jsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:dict];
    NSLog(@"json str is %@", jsonStr);
    return @"testEmit started";
}


@end
