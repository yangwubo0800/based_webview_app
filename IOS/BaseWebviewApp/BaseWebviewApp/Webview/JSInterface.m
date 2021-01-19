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
#import "../Utils/AFNetWorkingDemo.h"
#import "../AppDelegate.h"
#import "../Utils/UIDevice+TFDevice.h"
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import "../Utils/GPSConvertUtil.h"
#import "../Utils/Constant.h"
#import "BPush.h"
#import <GTSDK/GeTuiSdk.h>
#import "IFly.h"

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

// 切换屏幕横竖屏： 0 ，跟随旋转， 1 横屏， 2 竖屏
- (NSString *)setOrientation:(NSString *) msg{
    
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    if([@"0" isEqualToString:msg]){
        //只是设置跟随
        appDelegate.orientationMode = @"0";
    } else if([@"1" isEqualToString:msg]){
        appDelegate.orientationMode = @"1";
        //调用横屏代码
        [UIDevice switchNewOrientation:UIInterfaceOrientationLandscapeRight];
    }else if([@"2" isEqualToString:msg]){
        appDelegate.orientationMode = @"2";
        //调用横屏代码
        [UIDevice switchNewOrientation:UIInterfaceOrientationPortrait];
    }

    return  @"setOrientation started";
}
    
// 下载文件并预览
- (NSString *) downloadAndPreviewFile:(NSString *) jsonStr{
    NSError *error;
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonStr dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
    NSLog(@"downloadAndPreviewFile dict=%@", dict);
    NSArray *keys = [dict allKeys];
    NSString *url;
    NSString *fileName;
    for(int i=0; i<keys.count; i++){
        NSString *key = keys[i];
        if ([@"url" isEqualToString:key]){
            url = [dict valueForKey:key];
        }
        
        if([@"fileName" isEqualToString:key]){
            fileName = [dict valueForKey:key];
        }
    }
    
    NSLog(@"downloadAndPreviewFile url=%@ fileName=%@", url, fileName);
    if (url != nil) {
        WebviewController *wc = [WebviewController shareInstance];
        [wc downloadAndPreviewFile:url withName:fileName];
    }

    

    return  @"downloadAndPreviewFile started";
}

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


#pragma mark - checkoutUpdateAppVersion 校验是否需要前往APPStore更新
const NSString *appStoreAppID;//AppStore上面对应的APPID，获取方式如上图

- (NSString *)checkoutUpdateAppVersion :(NSString*)msg{
    appStoreAppID = msg;
    NSString *getAppStoreInfo = [NSString stringWithFormat:@"http://itunes.apple.com/cn/lookup?id=%@",appStoreAppID];
    //用AFNetwork网络请求方式发起Post请求
    AFHTTPSessionManager *manager = [AFHTTPSessionManager manager];
    [manager POST:getAppStoreInfo parameters:nil progress:nil success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
        NSArray *resultsArr = responseObject[@"results"];
        NSDictionary *dict = [resultsArr lastObject];
        /**  得到AppStore的应用的版本信息*/
        NSString *appStoreCurrentVersion = dict[@"version"];
        /**  获取当前安装的应用的版本信息*/
        NSString *appCurrentVersion = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"];
        NSLog(@"appCurrentVersion=%@ appStoreCurrentVersion=%@",appCurrentVersion, appStoreCurrentVersion);
        if(nil == appStoreCurrentVersion){
            NSLog(@"appstore无此应用");
            UIAlertView *updateAlertV = [[UIAlertView alloc] initWithTitle:@"" message:@"未查询到该应用信息！" delegate:nil cancelButtonTitle:@"确认" otherButtonTitles:nil, nil];
            [updateAlertV show];
        }else{
            if ([appCurrentVersion compare:appStoreCurrentVersion options:NSNumericSearch] == NSOrderedAscending){//有更新版本，需要提示前往更新
                UIAlertView *updateAlertV = [[UIAlertView alloc] initWithTitle:[NSString stringWithFormat:@"您有新版本更新(%@)", appStoreCurrentVersion] message:@"" delegate:self cancelButtonTitle:@"暂不更新" otherButtonTitles:@"马上更新", nil];
                [updateAlertV show];
            }else{//没有更新版本，不进行操作
                NSLog(@"当前为最新版本，暂无更新版本");
                UIAlertView *updateAlertV = [[UIAlertView alloc] initWithTitle:@"" message:@"当前已经是最新版本！" delegate:nil cancelButtonTitle:@"确认" otherButtonTitles:nil, nil];
                [updateAlertV show];
            }
        }
        
    } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
        NSLog(@"访问appStore失败");
    }];
    
    return @"checkoutUpdateAppVersion";
}


#pragma mark - UIAlertViewDelegate
- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *updateUrlString = [NSString stringWithFormat:@"https://itunes.apple.com/cn/app/id%@?mt=8",appStoreAppID];
    if (buttonIndex) {//马上更新
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:updateUrlString]];
    }else {//我在看看
        NSLog(@"用户决定暂时不更新");
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



//跳转地图进行导航，参数为起始位置经纬度和名称json字符
-(NSString *)doNavigation:(NSString *)locations
{
    //解析json格式参数
    NSError *error;
    NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[locations dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
    NSLog(@"doNavigation dict=%@", dict);
    NSArray *keys = [dict allKeys];
    
    NSString *slat;
    NSString *slon;
    NSString *sname;
    NSString *dlat;
    NSString *dlon;
    NSString *dname;
    for(int i=0; i<keys.count; i++){
        NSString *key = keys[i];
        if ([@"slat" isEqualToString:key]){
            slat = [dict valueForKey:key];
        }
        
        if([@"slon" isEqualToString:key]){
            slon = [dict valueForKey:key];
        }
        
        if([@"sname" isEqualToString:key]){
            sname = [dict valueForKey:key];
        }
        
        if([@"dlat" isEqualToString:key]){
            dlat = [dict valueForKey:key];
        }
        
        if([@"dlon" isEqualToString:key]){
            dlon = [dict valueForKey:key];
        }
        
        if([@"dname" isEqualToString:key]){
            dname = [dict valueForKey:key];
        }
    }
    
    NSLog(@"doNavigation slat=%@, slon=%@", slat, slon);
    // 坐标转换
    CLLocationCoordinate2D wgs84Start;
    wgs84Start.latitude = [slat doubleValue];
    wgs84Start.longitude = [slon doubleValue];
    
    CLLocationCoordinate2D wgs84Destination;
    wgs84Destination.latitude = [dlat doubleValue];
    wgs84Destination.longitude = [dlon doubleValue];
    
    CLLocationCoordinate2D gcj02Start = [GPSConvertUtil wgs84ToGcj02:wgs84Start];
    NSString *gcj02sLat = [NSString stringWithFormat:@"%f", gcj02Start.latitude];
    NSString *gcj02sLon = [NSString stringWithFormat:@"%f", gcj02Start.longitude];
    NSLog(@"出发地坐标转换为火星坐标后 gcj02sLat=%@, gcj02sLon=%@", gcj02sLat, gcj02sLon);
    
    
    CLLocationCoordinate2D gcj02Destination = [GPSConvertUtil wgs84ToGcj02:wgs84Destination];
    NSString *gcj02dLat = [NSString stringWithFormat:@"%f", gcj02Destination.latitude];
    NSString *gcj02dLon = [NSString stringWithFormat:@"%f", gcj02Destination.longitude];
    NSLog(@"目的地坐标转换为火星坐标后 gcj02dLat=%@, gcj02dLon=%@", gcj02dLat, gcj02dLon);

    
    NSArray *startLocation = [NSArray arrayWithObjects:gcj02sLat, gcj02sLon, sname, nil];
    NSArray *endLocation = [NSArray arrayWithObjects:gcj02dLat, gcj02dLon, dname, nil];
    


    NSMutableArray *maps = [NSMutableArray array];
    //苹果原生地图-苹果原生地图方法和其他不一样
    NSMutableDictionary *iosMapDic = [NSMutableDictionary dictionary];
    iosMapDic[@"title"] = @"苹果地图";
    [maps addObject:iosMapDic];

    //百度地图
    //http://lbsyun.baidu.com/index.php?title=uri/api/ios#service-page-anchor7
    if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"baidumap://"]]){
        NSMutableDictionary *baiduMapDic = [NSMutableDictionary dictionary];
        baiduMapDic[@"title"] = @"百度地图";
        //如果其实位置为0，则默认使用我的位置作为出发点
        NSString *urlString;
        if ([@"0" isEqualToString:slat]) {
            urlString = [[NSString stringWithFormat:@"baidumap://map/direction?origin={{我的位置}}&destination=latlng:%@,%@|name:%@&mode=driving&coord_type=gcj02", gcj02dLat, gcj02dLon, dname] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }else{
            urlString = [[NSString stringWithFormat:@"baidumap://map/direction?origin=latlng:%@,%@|name:%@&destination=latlng:%@,%@|name:%@&mode=driving&coord_type=gcj02", gcj02sLat, gcj02sLon, sname, gcj02dLat, gcj02dLon, dname] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }
        
        NSLog(@"百度地图urlString=%@", urlString);
        baiduMapDic[@"url"] = urlString;
        [maps addObject:baiduMapDic];
    }

    //高德地图
    //https://lbs.amap.com/api/amap-mobile/guide/ios/route
    if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"iosamap://"]]) {
        NSMutableDictionary *gaodeMapDic = [NSMutableDictionary dictionary];
        gaodeMapDic[@"title"] = @"高德地图";
        NSString *urlString;
        if ([@"0" isEqualToString:slat]) {
            urlString = [[NSString stringWithFormat:@"iosamap://path?sourceApplication=xxxx&did=&dlat=%@&dlon=%@&dname=%@&dev=0&t=0",   gcj02dLat, gcj02dLon, dname]stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }else{
            urlString = [[NSString stringWithFormat:@"iosamap://path?sourceApplication=xxxx&sid=&slat=%@&slon=%@&sname=%@&did=&dlat=%@&dlon=%@&dname=%@&dev=0&t=0",  gcj02sLat, gcj02sLon, sname, gcj02dLat, gcj02dLon, dname]stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }

        NSLog(@"高德地图urlString=%@", urlString);
        gaodeMapDic[@"url"] = urlString;
        [maps addObject:gaodeMapDic];
    }

    //谷歌地图
//    if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"comgooglemaps://"]]) {
//        NSMutableDictionary *googleMapDic = [NSMutableDictionary dictionary];
//        googleMapDic[@"title"] = @"谷歌地图";
//        NSString *urlString = [[NSString stringWithFormat:@"comgooglemaps://?x-source=%@&x-success=%@&saddr=&daddr=%@,%@&directionsmode=driving",@"导航测试",@"nav123456",endLocation[0], endLocation[1]] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
//        googleMapDic[@"url"] = urlString;
//        [maps addObject:googleMapDic];
//    }

    //腾讯地图
    //https://lbs.qq.com/webApi/uriV1/uriGuide/uriMobileRoute
    if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:@"qqmap://"]]) {
        NSMutableDictionary *qqMapDic = [NSMutableDictionary dictionary];
        qqMapDic[@"title"] = @"腾讯地图";
        
        NSString *urlString;
        if ([@"0" isEqualToString:slat]) {
            urlString = [[NSString stringWithFormat:@"qqmap://map/routeplan?from=我的位置&type=drive&tocoord=%@,%@&to=%@&coord_type=1&policy=0", gcj02dLat, gcj02dLon, dname]stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }else{
            urlString = [[NSString stringWithFormat:@"qqmap://map/routeplan?from=%@&fromcoord=%@,%@&type=drive&tocoord=%@,%@&to=%@&coord_type=1&policy=0", sname, gcj02sLat, gcj02sLon, gcj02dLat, gcj02dLon, dname]stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        }

        NSLog(@"腾讯地图urlString=%@", urlString);
        qqMapDic[@"url"] = urlString;
        [maps addObject:qqMapDic];
    }


    //选择
    UIAlertController * alert = [UIAlertController alertControllerWithTitle:@"地图选择" message:nil preferredStyle:UIAlertControllerStyleActionSheet];

    NSInteger index = maps.count;
    for (int i = 0; i < index; i++) {
        NSString * title = maps[i][@"title"];
        //苹果原生地图方法
        if (i == 0) {
            UIAlertAction * action = [UIAlertAction actionWithTitle:title style:(UIAlertActionStyleDefault) handler:^(UIAlertAction * _Nonnull action) {
                //[self navAppleMap];
                //[self navAppleMap:endLocation start:nil];
                if ([@"0" isEqualToString:slat]) {
                    [self navAppleMap:endLocation start:nil];
                }else{
                    [self navAppleMap:endLocation start:startLocation];
                }
            }];
            [alert addAction:action];
            continue;
        }


        UIAlertAction * action = [UIAlertAction actionWithTitle:title style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {

            NSString *urlString = maps[i][@"url"];
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:urlString]];
        }];
        [alert addAction:action];
    }

    WebviewController *wc = [WebviewController shareInstance];
    [wc presentViewController:alert animated:YES completion:nil];

    return @"doNavigationWithEndLocation started";
}





//苹果地图
- (void)navAppleMap:(NSArray *)endLocation start:(NSArray *)startLoaction
{
    //终点经纬度
    NSString *endLatStr = endLocation[0];
    double endLat = [endLatStr doubleValue];
    NSString *endLonStr = endLocation[1];
    double endLon = [endLonStr doubleValue];
    NSLog(@" endLat is %f endLon is %f", endLat, endLon);
    
    
    CLLocationCoordinate2D endLoc = CLLocationCoordinate2DMake(endLat, endLon);
    //用户位置
    MKMapItem *currentLoc = [MKMapItem mapItemForCurrentLocation];
    //终点位置
    MKMapItem *toLocation = [[MKMapItem alloc]initWithPlacemark:[[MKPlacemark alloc]initWithCoordinate:endLoc addressDictionary:nil] ];

    toLocation.name = endLocation[2];
    currentLoc.name = @"我的位置";
    
    NSArray *items;
    if (nil != startLoaction) {
        NSString *startLatStr = startLoaction[0];
        double startLat = [startLatStr doubleValue];
        NSString *startLonStr = startLoaction[1];
        double startLon = [startLonStr doubleValue];
        NSLog(@" startLat is %f startLon is %f", startLat, startLon);
        CLLocationCoordinate2D fromLoc = CLLocationCoordinate2DMake(startLat, startLon);
        //用户传入的起点位置
        MKMapItem *fromLocation = [[MKMapItem alloc]initWithPlacemark:[[MKPlacemark alloc]initWithCoordinate:fromLoc addressDictionary:nil]];
        fromLocation.name = startLoaction[2];
        items = @[fromLocation,toLocation];
    }else{
        items = @[currentLoc,toLocation];
    }

    
    //第一个
    NSDictionary *dic = @{
                          MKLaunchOptionsDirectionsModeKey : MKLaunchOptionsDirectionsModeDriving,
                          MKLaunchOptionsMapTypeKey : @(MKMapTypeStandard),
                          MKLaunchOptionsShowsTrafficKey : @(YES)
                          };
    
    [MKMapItem openMapsWithItems:items launchOptions:dic];
}


// 设置极光推送tag和点击消息跳转页面路径
- (NSString *) setJPushTagAndJumpUrl:(NSString *) tagWithUrl{
    @try {
        //解析json格式参数
        NSError *error;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[tagWithUrl dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
        NSLog(@"setJPushTagAndJumpUrl dict=%@", dict);
        NSArray *keys = [dict allKeys];
        
        NSString *tags;
        NSString *jumpUrl;
        for(int i=0; i<keys.count; i++){
            NSString *key = keys[i];
            if ([@"tags" isEqualToString:key]){
                tags = [dict valueForKey:key];
            }
            
            if([@"jumpUrl" isEqualToString:key]){
                jumpUrl = [dict valueForKey:key];
            }
        }
        
        //设置跳转url
        NSDictionary *urlDict = @{PUSH_MESSAGE_JUMP_URL_KEY:jumpUrl};
        NSString *urlJsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:urlDict];
        [self setValueByKey:urlJsonStr];
        
        //设置极光
        [JPushUtils setJPushTags:tags];
    } @catch (NSException *exception) {
        NSLog(@" exception happened");
    } @finally {
         NSLog(@"finally");
    }
 
    return @"setJPushTagAndJumpUrl started";
}


// 设置极光推送tag
- (NSString *) cleanJPushTag:(NSString *) msg{
    [JPushUtils cleanJPushTag];
    return @"cleanJPushTag started";
}


// 设置百度云推送tag和点击消息跳转页面路径
- (NSString *) setBaiduPushTagAndJumpUrl:(NSString *) tagWithUrl{
    @try {
        //解析json格式参数
        NSError *error;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[tagWithUrl dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
        NSLog(@"setJPushTagAndJumpUrl dict=%@", dict);
        NSArray *keys = [dict allKeys];
        
        NSString *tags;
        NSString *jumpUrl;
        for(int i=0; i<keys.count; i++){
            NSString *key = keys[i];
            if ([@"tags" isEqualToString:key]){
                tags = [dict valueForKey:key];
            }
            
            if([@"jumpUrl" isEqualToString:key]){
                jumpUrl = [dict valueForKey:key];
            }
        }
        
        //设置跳转url
        NSDictionary *urlDict = @{PUSH_MESSAGE_JUMP_URL_KEY:jumpUrl};
        NSString *urlJsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:urlDict];
        [self setValueByKey:urlJsonStr];
        
        //设置百度推送tag
        NSArray *tagArray = [tags componentsSeparatedByString:@","];
        NSLog(@"需要设置的tags=%@", tags);
        NSLog(@"需要设置的tagArray=%@", tagArray);
        
        // TODO: 先获取，在清除，最后设置
        [BPush listTagsWithCompleteHandler:^(id result, NSError *error) {
            // 获取标签组的返回值
            NSLog(@"获取百度云消息推送tags=%@", result);
            /*
            {
                "error_code" = 0;
                "request_id" = 3952581870;
                "response_params" =     {
                    "tag_num" = 3;
                    tags =         (
                                    {
                            "create_time" = 1600313565;
                            info = Mytag22648198;
                            "is_big_group" = 0;
                            name = Mytag;
                            tid = 268959189;
                            type = 2;
                        },
                                    {
                            "create_time" = 1600324494;
                            info = user0222648198;
                            "is_big_group" = 0;
                            name = user02;
                            tid = 268963091;
                            type = 2;
                        },
                                    {
                            "create_time" = 1600324500;
                            info = user0322648198;
                            "is_big_group" = 0;
                            name = user03;
                            tid = 268963093;
                            type = 2;
                        }
                    );
                };
            }
             */
         
            if (result) {
                NSDictionary *resultDict = result;
                NSDictionary *paramDict = resultDict[@"response_params"];
                NSArray *tagsResPonse = paramDict[@"tags"];
                NSMutableArray *needDeleteTags = [[NSMutableArray alloc]init];
                for(NSDictionary *temp in tagsResPonse){
                    NSString *name = temp[@"name"];
                    [needDeleteTags addObject:name];
                }
                
                //判断是否有要清除的tags
                if ([needDeleteTags count] == 0) {
                    NSLog(@"没有要清除的百度云消息推送tag");
                    [BPush setTags:tagArray withCompleteHandler:^(id result, NSError *error) {
                        if (result) {
                            NSLog(@"百度云消息推送设置tag成功 result=%@", result);
                        }
                    }];
                }else{
                    //清除
                     [BPush delTags:needDeleteTags withCompleteHandler:^(id result, NSError *error) {
                            NSLog(@"删除百度云消息推送tags result=%@", result);
                              //不管删除成功与否，都进行设置
                              [BPush setTags:tagArray withCompleteHandler:^(id result, NSError *error) {
                                  if (result) {
                                      NSLog(@"百度云消息推送设置tag成功 result=%@", result);
                                  }
                              }];
                    }];
                }
            }else{
                //没有要清除的tag可以直接设置
                [BPush setTags:tagArray withCompleteHandler:^(id result, NSError *error) {
                    if (result) {
                        NSLog(@"百度云消息推送设置tag成功 result=%@", result);
                    }
                }];
            }
        }];
        
    } @catch (NSException *exception) {
        NSLog(@" exception happened");
    } @finally {
         NSLog(@"finally");
    }
 
    return @"setBaiduPushTagAndJumpUrl started";
}

// 清除百度云消息推送tag
- (NSString *) cleanBaiduPushTag:(NSString *) msg{
     // TODO: 先获取，在清除
     [BPush listTagsWithCompleteHandler:^(id result, NSError *error) {
         // 获取标签组的返回值
         NSLog(@"获取百度云消息推送tags=%@", result);
         if (result) {
             NSDictionary *resultDict = result;
             NSDictionary *paramDict = resultDict[@"response_params"];
             NSArray *tagsResPonse = paramDict[@"tags"];
             NSMutableArray *needDeleteTags = [[NSMutableArray alloc]init];
             for(NSDictionary *temp in tagsResPonse){
                 NSString *name = temp[@"name"];
                 [needDeleteTags addObject:name];
             }
             
             if ([needDeleteTags count] == 0) {
                 NSLog(@"没有要清除的百度云消息推送tag");
             }else{
                 //清除
                 [BPush delTags:needDeleteTags withCompleteHandler:^(id result, NSError *error) {
                     NSLog(@"删除百度云消息推送tags result=%@", result);
                 }];
             }
         }else{
             //没有要清除的tag
         }
     }];
    return @"cleanBaiduPushTag started";
}

// 设置个推 tag和点击消息跳转页面路径
- (NSString *) setGTPushTagAndJumpUrl:(NSString *) tagWithUrl{
    @try {
        //解析json格式参数
        NSError *error;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[tagWithUrl dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
        NSLog(@"setJPushTagAndJumpUrl dict=%@", dict);
        NSArray *keys = [dict allKeys];
        
        NSString *tags;
        NSString *jumpUrl;
        for(int i=0; i<keys.count; i++){
            NSString *key = keys[i];
            if ([@"tags" isEqualToString:key]){
                tags = [dict valueForKey:key];
            }
            
            if([@"jumpUrl" isEqualToString:key]){
                jumpUrl = [dict valueForKey:key];
            }
        }
        
        //设置跳转url
        NSDictionary *urlDict = @{PUSH_MESSAGE_JUMP_URL_KEY:jumpUrl};
        NSString *urlJsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:urlDict];
        [self setValueByKey:urlJsonStr];
        
        //tag只能包含中文字符、英文字母、0-9、+-*.的组合（不支持空格）
        NSArray *tagArray = [tags componentsSeparatedByString:@","];
        [GeTuiSdk setTags:tagArray andSequenceNum:@"seqtag-1"];
    } @catch (NSException *exception) {
        NSLog(@" exception happened");
    } @finally {
         NSLog(@"finally");
    }
 
    return @"setJPushTagAndJumpUrl started";
}


- (NSString *) IFlyStartRecord:(NSString *) jsonParam{
    
    [[IFly sharedInstance] startRecord:jsonParam];
    return @"IFlyStartRecord";
}

@end
