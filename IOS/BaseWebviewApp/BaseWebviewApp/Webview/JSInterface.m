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
#import "../ImagePreviewViewController.h"
#import "../Utils/pingTool/WHPingTester.h"
#import "../UserGuide/RefreshNoNetworkView.h"
#import "../IJKPlayer/IJKMoviePlayerViewController.h"
#import "AFNetworking.h"
#import "../hikVideoPlayer/HikRealplayViewController.h"

//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)


static NSString* locationCallerName;

@interface JSInterface() <WHPingDelegate>

@property(nonatomic, strong) WHPingTester* pingTester;

@property(nonatomic, strong) NSString* videoCfgInfoUrl;


@end

@implementation JSInterface

//(id) handler:(id) msg
//参数可以是任何类型, 但是返回值类型不能为 void。 如果不需要参数，也必须声明，声明后不使用就行

// 平台检测，提供给前端判断当前运行环境
- (NSString *) platformCheck:(NSString *) msg{
    return  @"ios";
}

// 获取唯一标识
- (NSString *) getDeviceId:(NSString *) msg{
    NSString *deviceId = [LZKeychain getDeviceIDInKeychain];
    NSLog(@"getDeviceId deviceId is %@", deviceId);
    return  deviceId;
}


// 直播
- (NSString *) ijkLivePlay:(NSString *) playPath{
    WebviewController *wc = [WebviewController shareInstance];
    [wc ijkLivePlay:playPath withTitle:@""];
    return  @"ijkLivePlay started";
}


// ijk播放，带有视频标题
- (NSString *) ijkLivePlayWithTitle:(NSString*) jsonStr{
    NSLog(@"ijkLivePlayWithTitle jsonStr=%@", jsonStr);
    NSError *error;
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonStr dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
    //NSDictionary *dict = jsonStr;
    NSLog(@"ijkLivePlayWithTitle dict=%@", dict);
    NSArray *keys = [dict allKeys];
    NSString *url;
    NSString *videoTitle;
    for(int i=0; i<keys.count; i++){
        NSString *key = keys[i];
        if ([@"url" isEqualToString:key]){
            url = [dict valueForKey:key];
        }
        
        if([@"videoTitle" isEqualToString:key]){
            videoTitle = [dict valueForKey:key];
        }
    }
    NSLog(@"ijkLivePlayWithTitle url=%@ videoTitle=%@", url, videoTitle);
    if (url != nil) {
        WebviewController *wc = [WebviewController shareInstance];
        [wc ijkLivePlay:url withTitle:videoTitle];
    }

    return  @"ijkLivePlayWithTitle started";
}


// 海康视频播放
- (NSString *) hikVideoPlay:(NSString*) jsonStr{
    NSLog(@"hikVideoPlay jsonStr=%@", jsonStr);
    NSError *error;
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonStr dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
    //NSDictionary *dict = jsonStr;
    NSLog(@"hikVideoPlay dict=%@", dict);
    NSArray *keys = [dict allKeys];
    NSString *url;
    NSString *videoTitle;
    for(int i=0; i<keys.count; i++){
        NSString *key = keys[i];
        if ([@"url" isEqualToString:key]){
            url = [dict valueForKey:key];
        }
        
        if([@"videoTitle" isEqualToString:key]){
            videoTitle = [dict valueForKey:key];
        }
    }
    NSLog(@"hikVideoPlay url=%@ videoTitle=%@", url, videoTitle);
    if (url != nil) {
        WebviewController *wc = [WebviewController shareInstance];
        [HikRealplayViewController presentFromViewController:wc URL:url videoTitle:videoTitle];
    }
    return  @"hikVideoPlay started";
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
    wc.scanCallerName = callerName;
    [[CameraController shareInstance] scanQRCode:wc];
    return @"scanQRCode started";
}

// 定位
- (NSString *) locate:(NSString *) callerName{
    WebviewController *wc = [WebviewController shareInstance];
    wc.locateCallerName = callerName;
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
//    BOOL networkAvailable = [checkNetwork checkNetworkCanUse];
//    return (networkAvailable == YES) ? @"YES": @"NO";
    
    // TODO: 将访问苹果网站改为ping baidu,放置局域网的限制策略，误判网络不可用
    NSLog(@"=====checkNetwork started");
    self.pingTester = [[WHPingTester alloc] initWithHostName:@"www.baidu.com"];
    self.pingTester.delegate = self;
    [self.pingTester startPing];
    // 此处还是无法异步返回，因为结果在其他回调中才知道
    return @"checkNetwork started";
    
}

#pragma delegate
-(void)didPingSucccessWithTime:(float)time withError:(NSError *)error{
    NSLog(@"=====didPingSucccessWithTime");
    NSDictionary *dict;
    WebviewController *wc = [WebviewController shareInstance];
    if (error) {
        NSLog(@"=====didPingSucccessWithTime error=%@",error);
        dict = @{@"NetworkAvailable":@"NO"};
        //TODO: 加载无网络页面
        NSLog(@" show no network UI");
        RefreshNoNetworkView *rv = [[RefreshNoNetworkView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)];
        [wc.view addSubview:rv];
        
    }else{
        //返回网络正常给前端
        NSLog(@"ping网络正常");
        dict = @{@"NetworkAvailable":@"YES"};
    }
    
    [self.pingTester stopPing];
    

    NSLog(@"ping resut is %@",dict);
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"checkNetwork";
    NSString *JsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:dict];
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"','"];
    js = [js stringByAppendingString:JsonStr];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
}

// 启动横屏显示实时画面
- (NSString *) startRealHtml:(NSString*) htmlPath{
//    NSString *path = htmlPath;
//    //转义字符或字符串中含有中文， 都可能导致url=nil,需要处理
//    NSString * urlStr = [path stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
//    NSURL *url = [NSURL URLWithString:urlStr];
    NSURL *url = [NSURL URLWithString:htmlPath];
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


// 图片预览
- (NSString *) imagePreview:(NSString*) imageUrl{
    [[ImagePreviewViewController shareInstance] show:imageUrl];
    NSLog(@" imagePreview");
    return @"imagePreview started";
}

// 定时设置视频配置信息, 传递后台数据获取的url过来，返回时关闭定时器
- (NSString *) setVideoConfigInfoGetUrl:(NSString*) url{
    self.videoCfgInfoUrl = url;
    NSLog(@"setVideoConfigInfo url is %@", url);
    IJKVideoViewController *ijkVideo = [IJKVideoViewController getInstance];
    NSLog(@" setVideoConfigInfo ijkVideo=%@", ijkVideo);
    //由于不知道视频什么时候会完成加载显示，故启动时直接获取后台数据设置可能无效，将url设置到视频播放界面，由界面自行获取数据来设置
    ijkVideo.videoInfoUrl = url;
    //创建定时器， 销毁定时器在退出界面之前，在ijkMoviePlay里面
    ijkVideo.timer =  [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(updateVideoCfgInfo) userInfo:nil repeats:YES];
    
    return @"setVideoConfigInfoGetUrl started";
}

//海康视频配置信息更新需要新增一个借口，启用其自身定时器来实现更新界面
- (NSString *) setHikVideoConfigInfoGetUrl:(NSString*) url{
    self.videoCfgInfoUrl = url;
    NSLog(@"setHikVideoConfigInfoGetUrl url is %@", url);
    HikRealplayViewController *hikVideo = [HikRealplayViewController getInstance];
    NSLog(@" setHikVideoConfigInfoGetUrl hikVideo=%@", hikVideo);
    hikVideo.videoInfoUrl = url;
    //创建定时器， 销毁定时器在退出界面之前，在ijkMoviePlay里面
    hikVideo.timer =  [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(updateHikVideoCfgInfo) userInfo:nil repeats:YES];
    
    return @"setHikVideoConfigInfoGetUrl started";
}


-(void)updateHikVideoCfgInfo{
    if (self.videoCfgInfoUrl) {
        AFHTTPSessionManager *manager =[AFHTTPSessionManager manager];
        NSLog(@"-------start get updateVideoCfgInfo .....");
        // parameters 参数字典
        [manager GET:self.videoCfgInfoUrl parameters:nil progress:^(NSProgress * _Nonnull downloadProgress) {
            //进度
            NSLog(@" downloadProgress is %lld", downloadProgress.completedUnitCount);
        } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
            // task 我们可以通过task拿到响应头
            NSLog(@" success response is %@", task.response);
            // responseObject:请求成功返回的响应结果（AFN内部已经把响应体转换为OC对象，通常是字典或数组)
            NSLog(@" success responseObject is %@", responseObject);
            //TODO: 解析后台返回数据，并更新
            NSDictionary *dictResponse =  responseObject;
            NSDictionary *data = [dictResponse objectForKey:@"data"];
            NSString *showInfo=@"";
            NSArray *keys = [data allKeys];
            for (int i=0; i< [keys count]; i++) {
                NSString *key = keys[i];
                NSString *value = [data objectForKey:key];
                showInfo = [NSString stringWithFormat:@"%@：%@\n%@", key, value, showInfo];
            }
            
            //NSString* info = [StringUtils UIUtilsFomateJsonWithDictionary:responseObject];
            HikRealplayViewController *hikVideo = [HikRealplayViewController getInstance];
            NSLog(@" setVideoConfigInfo hikVideo=%@", hikVideo);
            NSLog(@" setVideoConfigInfo showInfo=%@", showInfo);
            if (hikVideo) {
                [hikVideo.videoConfigInfo setText:showInfo];
                [hikVideo.videoConfigInfo setBackgroundColor:[[UIColor blackColor]colorWithAlphaComponent:0.5f]];
                [hikVideo.videoConfigInfo setTextColor:[UIColor whiteColor]];
                hikVideo.videoConfigInfo.numberOfLines = 0;
                hikVideo.videoConfigInfo.lineBreakMode = NSLineBreakByTruncatingTail;
                CGSize maxLabelSize = CGSizeMake(200, 9999);
                CGSize expectSize = [hikVideo.videoConfigInfo sizeThatFits:maxLabelSize];
                hikVideo.videoConfigInfo.frame = CGRectMake(20, 50, expectSize.width, expectSize.height);
            }
        } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
            // error 错误信息
            NSLog(@" failure response is %@", task.response);
            NSLog(@" failure error is %@", error.description);
        }];
    }else{
        NSLog(@"The video config info url is nil");
    }
}



//ios框架自主获取后台视频配置信息进行更新
-(void)updateVideoCfgInfo{
    if (self.videoCfgInfoUrl) {
        AFHTTPSessionManager *manager =[AFHTTPSessionManager manager];
        NSLog(@"-------start get updateVideoCfgInfo .....");
        // parameters 参数字典
        [manager GET:self.videoCfgInfoUrl parameters:nil progress:^(NSProgress * _Nonnull downloadProgress) {
            //进度
            NSLog(@" downloadProgress is %lld", downloadProgress.completedUnitCount);
        } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
            // task 我们可以通过task拿到响应头
            NSLog(@" success response is %@", task.response);
            // responseObject:请求成功返回的响应结果（AFN内部已经把响应体转换为OC对象，通常是字典或数组)
            NSLog(@" success responseObject is %@", responseObject);
            //TODO: 解析后台返回数据，并更新
            NSDictionary *dictResponse =  responseObject;
            NSDictionary *data = [dictResponse objectForKey:@"data"];
            NSString *showInfo=@"";
            NSArray *keys = [data allKeys];
            for (int i=0; i< [keys count]; i++) {
                NSString *key = keys[i];
                NSString *value = [data objectForKey:key];
                showInfo = [NSString stringWithFormat:@"%@：%@\n%@", key, value, showInfo];
            }
            
            //NSString* info = [StringUtils UIUtilsFomateJsonWithDictionary:responseObject];
            IJKVideoViewController *ijkVideo = [IJKVideoViewController getInstance];
            NSLog(@" setVideoConfigInfo ijkVideo=%@", ijkVideo);
            NSLog(@" setVideoConfigInfo showInfo=%@", showInfo);
            if (ijkVideo) {
                [ijkVideo.videoConfigInfo setText:showInfo];
                [ijkVideo.videoConfigInfo setBackgroundColor:[[UIColor blackColor]colorWithAlphaComponent:0.5f]];
                [ijkVideo.videoConfigInfo setTextColor:[UIColor whiteColor]];
                ijkVideo.videoConfigInfo.numberOfLines = 0;
                ijkVideo.videoConfigInfo.lineBreakMode = NSLineBreakByTruncatingTail;
                CGSize maxLabelSize = CGSizeMake(200, 9999);
                CGSize expectSize = [ijkVideo.videoConfigInfo sizeThatFits:maxLabelSize];
                ijkVideo.videoConfigInfo.frame = CGRectMake(20, 50, expectSize.width, expectSize.height);
            }
        } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
            // error 错误信息
            NSLog(@" failure response is %@", task.response);
            NSLog(@" failure error is %@", error.description);
        }];
    }else{
        NSLog(@"The video config info url is nil");
    }
}


// 测试调用js 触发前端监听事件
- (NSString *) testEmit:(NSString *) callerName{
    WebviewController *wc = [WebviewController shareInstance];
    NSString *emitName = @"location";
    //NSString *locationJson = @"empty";
    NSString *locationJson = @"{\"address\":\"中国湖南省长沙市岳麓区佳园路\",\"latitude\":\"28.229733\",\"longitude\":\"112.864245\",\"name\":\"test1\"}";
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
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
