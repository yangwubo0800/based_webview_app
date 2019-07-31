//
//  WebviewController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/6.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "WebviewController.h"
#import <WebKit/WebKit.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <Photos/Photos.h>
#import "../CameraController.h"
#import <AVFoundation/AVFoundation.h>
#import "../LocationController.h"
#import "../LZKeychain.h"
#import "../Utils/checkNetwork.h"
#import "../Utils/StringUtils.h"
#import <Foundation/NSURLError.h>
#import "../UserGuide/NoNetworkView.h"
#import "../UserGuide/RefreshNoNetworkView.h"
#import "JPushUtils.h"
#import "../IJKPlayer/IJKMoviePlayerViewController.h"
#import "../IJKPlayer/VideoPlayViewController.h"
#import "../Utils/AFNetWorkingDemo.h"


//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)

#define iPhoneX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) : NO)

//状态栏和导航栏高度
//通常 状态栏+导航栏=20+44=64
//iPhone X 状态栏+导航栏=44+44=88
#define StatusBarAndNavigationBarHeight (iPhoneX ? 88.f : 64.f)

#define LOAD_LOCAL_HTML YES

//单例模式
static WebviewController *instance = nil;


#pragma mark 此部分为使用系统自带的执行js方法需要实现的delegate，目前在代码中已经不用。
//WKWebView 内存不释放的问题解决
@interface WeakWebViewScriptMessageDelegate : NSObject<WKScriptMessageHandler>
//WKScriptMessageHandler 这个协议类专门用来处理JavaScript调用原生OC的方法
@property (nonatomic, weak) id<WKScriptMessageHandler> scriptDelegate;
- (instancetype)initWithDelegate:(id<WKScriptMessageHandler>)scriptDelegate;
@end


@implementation WeakWebViewScriptMessageDelegate

- (instancetype)initWithDelegate:(id<WKScriptMessageHandler>)scriptDelegate {
    self = [super init];
    NSLog(@" initWithDelegate ");
    if (self) {
        _scriptDelegate = scriptDelegate;
    }
    return self;
}

#pragma mark - WKScriptMessageHandler
//遵循WKScriptMessageHandler协议，必须实现如下方法，然后把方法向外传递
//通过接收JS传出消息的name进行捕捉的回调方法
- (void)userContentController:(WKUserContentController *)userContentController didReceiveScriptMessage:(WKScriptMessage *)message {
    
    if ([self.scriptDelegate respondsToSelector:@selector(userContentController:didReceiveScriptMessage:)]) {
        [self.scriptDelegate userContentController:userContentController didReceiveScriptMessage:message];
    }
}

@end




@interface WebviewController () <WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler>

//webview
@property(nonatomic, strong) WKWebView *webView;
//进度条
@property(nonatomic, strong) UIProgressView *progressView;



@end

@implementation WebviewController

// 单例模式实现
+(WebviewController *)shareInstance{
    if (instance == nil) {
        instance = [[WebviewController alloc] init];
    }
    
    return instance;
}

//限制方法，类只能初始化一次
//alloc的时候调用
+ (id) allocWithZone:(struct _NSZone *)zone{
    if(instance == nil){
        instance = [super allocWithZone:zone];
    }
    return instance;

}

//拷贝方法
- (id)copyWithZone:(NSZone *)zone{
    return instance;
}

//提给为网络恢复时重新加载页面使用
-(void)reloadWebview{
    
    NSLog(@" reload webview _webView=%p", _webView);
    if (_webView != nil && _currentUrl != nil) {
        NSLog(@" reload webview _currentUrl=%@", _currentUrl);
        NSURLRequest *request =[NSURLRequest requestWithURL:_currentUrl];
        [_webView loadRequest:request];

    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    //检查网络
    //[checkNetwork checkNetworkCanUse];
    // 设置导航栏
    //[self setupNavigationItem];
    // 纯网页显示测试用
    //[self loadBasicWebview];
    
    //添加webview 和 进度条显示
    [self.view addSubview:self.webView];
    [self.view addSubview:self.progressView];
    
    //添加监测网页加载进度的观察者
    [self.webView addObserver:self
                   forKeyPath:NSStringFromSelector(@selector(estimatedProgress))
                      options:0
                      context:nil];
    
    [self.webView addObserver:self
                   forKeyPath:@"title"
                      options:NSKeyValueObservingOptionNew
                      context:nil];
    //监听URL变化
    [self.webView addObserver:self
                  forKeyPath:@"URL"
                     options:NSKeyValueObservingOptionNew
                     context:nil];
    
}



#pragma mark 进度值以及其他监听对象变化，会调用此方法
//kvo 监听进度 必须实现此方法
-(void)observeValueForKeyPath:(NSString *)keyPath
                     ofObject:(id)object
                       change:(NSDictionary<NSKeyValueChangeKey,id> *)change
                      context:(void *)context{
    
    if ([keyPath isEqualToString:NSStringFromSelector(@selector(estimatedProgress))]
        && object == _webView) {
        
        NSLog(@"webview load progress = %f",_webView.estimatedProgress);
        self.progressView.progress = _webView.estimatedProgress;
        // 加载完成则隐藏进度条
        if (_webView.estimatedProgress >= 1.0f) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.3 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
                NSLog(@"set progress to 0 ");
                self.progressView.progress = 0;
            });
        }
        
    }else if([keyPath isEqualToString:@"title"]
             && object == _webView){
        //响应适配html中的title属性，即在weview加载的时候除了读取进度上报，也能将title报上来
        NSLog(@" set naviagtion item tile to %@", _webView.title);
        self.navigationItem.title = _webView.title;
    }else if([keyPath isEqualToString:@"URL"]
             && object == _webView){
        //网页URL
        NSLog(@"#####NOW URL IS %@", _webView.URL);
        if (_webView.URL != nil) {
            self.currentUrl = _webView.URL;
            NSLog(@"set current url is %@", self.currentUrl);
        }
    }else{
        NSLog(@" observeValueForKeyPath call super method");
        [super observeValueForKeyPath:keyPath
                             ofObject:object
                               change:change
                              context:context];
    }
}


- (void)setupNavigationItem{
    // 刷新按钮
    UIButton * refreshButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [refreshButton setBackgroundColor:[UIColor blueColor]];
    
    //debug image resource visit.
    NSString *imagePath = [[NSBundle mainBundle] pathForResource:@"webRefreshButton@3x.png" ofType:nil inDirectory:@"Resource"];
    NSLog(@"=====image path is %@", imagePath);
    NSString *imagePath1 = [[NSBundle mainBundle] pathForResource:@"webRefreshButton@3x"  ofType:@"png"];
    NSLog(@"=====image path1 is %@", imagePath1);
    
    [refreshButton setImage:[UIImage imageWithContentsOfFile:imagePath] forState:UIControlStateNormal];
    //[refreshButton setImage:[UIImage imageNamed:@"webRefreshButton"] forState:UIControlStateNormal];
    
    [refreshButton addTarget:self action:@selector(refreshAction:) forControlEvents:UIControlEventTouchUpInside];
    refreshButton.frame = CGRectMake(0, 0, 30, StatusBarAndNavigationBarHeight);
    
    UIBarButtonItem * refreshButtonItem = [[UIBarButtonItem alloc] initWithCustomView:refreshButton];
    
    UIBarButtonItem * ocToJs = [[UIBarButtonItem alloc] initWithTitle:@"OC调用JS" style:UIBarButtonItemStyleDone target:self action:@selector(ocToJs)];
    
    self.navigationItem.rightBarButtonItems = @[refreshButtonItem, ocToJs];
    
}


- (void)refreshAction:(id)sender{
    NSLog(@" webview reload ");
    [_webView reload];
}

- (void)ocToJs{
    
    //OC调用JS
    [_bridge callHandler:@"changeColor" data:@{ @"zhangsan":@"lisi" }];
    
    //changeColor()是JS方法名，completionHandler是异步回调block
//    NSString *jsString = [NSString stringWithFormat:@"changeColor('%@')", @"Js参数"];
//    [_webView evaluateJavaScript:jsString completionHandler:^(id _Nullable data, NSError * _Nullable error){
//        NSLog(@"change color again");
//    }];
    
    //改变字体大小 调用原生JS方法
//    NSString *jsFont = [NSString stringWithFormat:@"document.getElementsByTagName('body')[0].style.webkitTextSizeAdjust= '%d%%'", arc4random()%99 + 100];
//    [_webView evaluateJavaScript:jsFont completionHandler:nil];
//
//    NSString * path =  [[NSBundle mainBundle] pathForResource:@"girl" ofType:@"png"];
//    NSString *jsPicture = [NSString stringWithFormat:@"changePicture('%@','%@')", @"pictureId",path];
//    [_webView evaluateJavaScript:jsPicture completionHandler:^(id _Nullable data, NSError * _Nullable error) {
//        NSLog(@"切换本地头像");
//    }];
    
}


-(void)loadBasicWebview{
    // 1.创建webview，并设置大小，"20"为状态栏高度
    CGFloat width = self.view.frame.size.width;
    CGFloat height = self.view.frame.size.height - 20;
    //UIWebView *webView = [[UIWebView alloc] initWithFrame:CGRectMake(0, 20, width, height)];
    WKWebView *webView = [[WKWebView alloc] initWithFrame:CGRectMake(0, 20, width, height)];
    
    webView.allowsBackForwardNavigationGestures = YES;
    
    
    // 2.创建URL
    NSURL *url = [NSURL URLWithString:@"http://www.baidu.com"];
    // 3.创建Request
    NSURLRequest *request =[NSURLRequest requestWithURL:url];
    // 4.加载网页
    [webView loadRequest:request];
    // 5.最后将webView添加到界面
    [self.view addSubview:webView];
}


#pragma mark getter

- (UIProgressView *)progressView
{
    if (!_progressView){
        //设置进度条位置和大小
        _progressView = [[UIProgressView alloc] initWithFrame:CGRectMake(0, StatusBarAndNavigationBarHeight + 1, self.view.frame.size.width, 2)];
        //颜色与安卓平台保持一致
        _progressView.tintColor = [UIColor blueColor];
        _progressView.trackTintColor = [UIColor clearColor];
    }
    return _progressView;
}


- (WKWebView *)webView{
    if(_webView == nil){
        
        //创建网页配置对象
        WKWebViewConfiguration *config = [[WKWebViewConfiguration alloc] init];
        
        // 创建设置对象
        WKPreferences *preference = [[WKPreferences alloc]init];
        //最小字体大小 当将javaScriptEnabled属性设置为NO时，可以看到明显的效果
        preference.minimumFontSize = 0;
        //设置是否支持javaScript 默认是支持的
        preference.javaScriptEnabled = YES;
        // 在iOS上默认为NO，表示是否允许不经过用户交互由javaScript自动打开窗口
        preference.javaScriptCanOpenWindowsAutomatically = YES;
        config.preferences = preference;
        
        // 是使用h5的视频播放器在线播放, 还是使用原生播放器全屏播放
        
        config.allowsInlineMediaPlayback = YES;
        //设置视频是否需要用户手动播放  设置为NO则会允许自动播放
        config.mediaTypesRequiringUserActionForPlayback = YES;
        //设置是否允许画中画技术 在特定设备上有效
        config.allowsPictureInPictureMediaPlayback = YES;
//        //设置请求的User-Agent信息中应用程序名称 iOS9后可用
//        config.applicationNameForUserAgent = @"ChinaDailyForiPad";
//
//        //自定义的WKScriptMessageHandler 是为了解决内存不释放的问题
//        WeakWebViewScriptMessageDelegate *weakScriptMessageDelegate = [[WeakWebViewScriptMessageDelegate alloc] initWithDelegate:self];
//
//
//        //这个类主要用来做native与JavaScript的交互管理
//        WKUserContentController * wkUController = [[WKUserContentController alloc] init];
//        //注册一个name为jsToOcNoPrams的js方法 设置处理接收JS方法的对象
//        [wkUController addScriptMessageHandler:weakScriptMessageDelegate  name:@"jsToOcNoPrams"];
//        [wkUController addScriptMessageHandler:weakScriptMessageDelegate  name:@"jsToOcWithPrams"];
//        [wkUController addScriptMessageHandler:weakScriptMessageDelegate  name:@"jsToOcCallCamera"];
//        [wkUController addScriptMessageHandler:weakScriptMessageDelegate  name:@"jsToOcCallLocation"];
//
//        config.userContentController = wkUController;
//
//        //以下代码适配文本大小
//        NSString *jSString = @"var meta = document.createElement('meta'); meta.setAttribute('name', 'viewport'); meta.setAttribute('content', 'width=device-width'); document.getElementsByTagName('head')[0].appendChild(meta);";
//        //用于进行JavaScript注入
//        WKUserScript *wkUScript = [[WKUserScript alloc] initWithSource:jSString injectionTime:WKUserScriptInjectionTimeAtDocumentEnd forMainFrameOnly:YES];
//        [config.userContentController addUserScript:wkUScript];

        _webView = [[WKWebView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT) configuration:config];
        // UI代理
        _webView.UIDelegate = self;
        // 导航代理
        _webView.navigationDelegate = self;
        
        // java script bridge
        [WebViewJavascriptBridge enableLogging];
        _bridge = [WebViewJavascriptBridge bridgeForWebView:_webView];
        [_bridge setWebViewDelegate:self];
        
//        [_bridge registerHandler:@"testObjcCallback" handler:^(id data, WVJBResponseCallback responseCallback) {
//            NSLog(@"testObjcCallback called: %@", data);
//            responseCallback(@"Response from testObjcCallback");
//        }];
//        
//        [_bridge callHandler:@"testJavascriptHandler" data:@{ @"foo":@"before ready" }];
        
        //无参数
        [_bridge registerHandler:@"jsCallWithOutData" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallWithOutData called: %@", data);
            responseCallback(@"Response from jsCallWithOutData");
        }];
        
        //有参数
        [_bridge registerHandler:@"jsCallWithData" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallWithData called: %@", data);
            responseCallback(@"Response from jsCallWithData");
        }];
        
        //获取唯一识别码
        [_bridge registerHandler:@"jsCallGetUUID" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallGetUUID called: %@", data);
            NSString *deviceId = [LZKeychain getDeviceIDInKeychain];
            NSLog(@"device id is %@", deviceId);
            responseCallback(deviceId);
        }];
        
        //拍照
        [_bridge registerHandler:@"jsCallTakePhoto" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallTakePhoto called: %@", data);
            [[CameraController shareInstance] nativeTakePhoto:self];
            responseCallback(@"take photo started");
        }];
        
        
        //录像
        [_bridge registerHandler:@"jsCallRecordVideo" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallRecordVideo called: %@", data);
            [[CameraController shareInstance] nativeRecordVideo:self];
            responseCallback(@"record video started");
        }];
        
        //扫码
        [_bridge registerHandler:@"jsCallScanQRCode" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallScanQRCode called: %@", data);
            [[CameraController shareInstance] scanQRCode:self];
            responseCallback(@"scan qr code started");
        }];
        
        //定位
        [_bridge registerHandler:@"jsCallLocate" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallLocate called: %@", data);
            [[LocationController shareInstance] locate:self];
            responseCallback(@"locate started");
        }];
        
        //极光分类推送，根据站点stationId 作为tag来设置接受推送消息, data 为用逗号隔开的分类tags.
        [_bridge registerHandler:@"jsSetJPushTag" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsSetJPushTag called: %@", data);
            [JPushUtils setJPushTags:data];
            responseCallback(@"set JPush tag started");
        }];
        
        //使用ijkplayer播放器直播
        [_bridge registerHandler:@"jsCallLivePlay" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallLivePlay called: %@", data);
            [self ijkLivePlay:data];
            responseCallback(@"call live play started");
        }];
        
        
        //使用ijkplayer播放器录播
        [_bridge registerHandler:@"jsCallVideoPlay" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallVideoPlay called: %@", data);
            [self ijkVideoPlay:data];
            responseCallback(@"call video play started");
        }];
        
        //使用AFNetworking get
        [_bridge registerHandler:@"jsCallGet" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallGet called: %@", data);
            [AFNetWorkingDemo AFNGet];
            responseCallback(@"afn get started");
        }];
        
        //使用AFNetworking post
        [_bridge registerHandler:@"jsCallPost" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallPost called: %@", data);
            [AFNetWorkingDemo AFNPost];
            responseCallback(@"afn post started");
        }];

        
        //拨打电话功能
        [_bridge registerHandler:@"jsCallPhoneNumber" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallPhoneNumber called: %@", data);
            NSMutableString *str=[[NSMutableString alloc]initWithFormat:@"tel:%@",data];
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:str]];
            responseCallback(@" Call Phone Number started");
        }];

        //根据key设置value, js传递过来的参数以json格式{key:xxx, value:xxx}
        [_bridge registerHandler:@"jsCallSetValue" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallSetValue called: %@", data);
            NSError *error;
//            NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
            NSDictionary *dict = data;
            NSLog(@"=====dict=%@", dict);
            NSArray *keys = [dict allKeys];
            for(int i=0; i<keys.count; i++){
                NSString *key = keys[i];
                NSString *value = [dict valueForKey:key];
                NSLog(@"key is %@, value is %@", key, value);
                [[NSUserDefaults standardUserDefaults] setObject:value  forKey:key];
            }
            [[NSUserDefaults standardUserDefaults] synchronize];
            responseCallback(@" set value started");
        }];
        
        //根据key获取value, data 为 key
        [_bridge registerHandler:@"jsCallGetValue" handler:^(id data, WVJBResponseCallback responseCallback){
            NSLog(@" jsCallGetValue called: %@", data);
            NSString *value = [[NSUserDefaults standardUserDefaults] objectForKey:data];
            responseCallback(value);
        }];
        

        // 使用宏来控制是加载本地页面还是网络请求
        if (LOAD_LOCAL_HTML) {
            NSString *htmlPath = [[NSBundle mainBundle] pathForResource:@"webviewJavascriptTest.html" ofType:nil inDirectory:@"Resource"];
            NSLog(@"LOAD_LOCAL_HTML htmlPath=%@", htmlPath);
    
            NSData *data = [[NSData alloc] initWithContentsOfFile:htmlPath];
            NSString *bundlePath = [[NSBundle mainBundle]bundlePath];
            NSURL *baseUrl = [NSURL fileURLWithPath:bundlePath];
            NSLog(@"the whole project resouce bundlePath is %@", bundlePath);
    
            if (@available(iOS 9.0, *)) {
                [_webView loadData:data MIMEType:@"text/html" characterEncodingName:@"UTF-8" baseURL:baseUrl];
            } else {
                // Fallback on earlier versions
                NSLog(@"IOS version is lower 9.0, can not load local html");
                NSString *htmlString = [[NSString alloc]initWithContentsOfFile:htmlPath encoding:NSUTF8StringEncoding error:nil];
                [_webView loadHTMLString:htmlString baseURL:[NSURL fileURLWithPath:bundlePath]];
            }
        } else {
            // 2.创建URL
            NSString *path = @"http://www.baidu.com";
            NSURL *url = [NSURL URLWithString:path];
            if (url != nil) {
                self.currentUrl = url;
                NSLog(@" first time create webview current url is %@", self.currentUrl);
            }
            // 3.创建Request
            NSURLRequest *request =[NSURLRequest requestWithURL:url];
            // 4.加载网页
            [_webView loadRequest:request];
        }
        
    }
    return _webView;
}


- (void)ijkLivePlay:(NSString *)livePath {
    //萤石云
    NSString *path = @"http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //CCTV1
    //NSString *path = @"http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8";
    // https
    //NSString *path  = @"https://media.w3.org/2010/05/sintel/trailer.mp4";
    //big duck
    //NSString *path = @"https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8";
    
    NSURL *url;
    if (livePath == nil) {
        url = [NSURL URLWithString:path];
    } else{
        url = [NSURL URLWithString:livePath];
    }
    
    NSString *scheme = [[url scheme] lowercaseString];
    
    if ([scheme isEqualToString:@"http"]
        || [scheme isEqualToString:@"https"]
        || [scheme isEqualToString:@"rtmp"]) {
        [IJKVideoViewController presentFromViewController:self withTitle:[NSString stringWithFormat:@"URL: %@", url] URL:url completion:^{
            //            [self.navigationController popViewControllerAnimated:NO];
        }];
    }
}

- (void)ijkVideoPlay:(NSString*)videoPath {
    //萤石云
    //NSString *path = @"http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //CCTV1
    //NSString *path = @"http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8";
    // https
    //NSString *path  = @"https://media.w3.org/2010/05/sintel/trailer.mp4";
    //big duck
    NSString *path = @"https://video-dev.github.io/streams/x36xhzz/x36xhzz.m3u8";
    
    NSURL *url;
    if (videoPath == nil) {
        url = [NSURL URLWithString:path];
    } else{
        url = [NSURL URLWithString:videoPath];
    }
    
    NSString *scheme = [[url scheme] lowercaseString];
    
    if ([scheme isEqualToString:@"http"]
        || [scheme isEqualToString:@"https"]
        || [scheme isEqualToString:@"rtmp"]) {
        [VideoPlayViewController presentFromViewController:self withTitle:[NSString stringWithFormat:@"URL: %@", url] URL:url completion:^{
            //            [self.navigationController popViewControllerAnimated:NO];
        }];
    }
}

//被自定义的WKScriptMessageHandler在回调方法里通过代理回调回来，绕了一圈就是为了解决内存不释放的问题
//通过接收JS传出消息的name进行捕捉的回调方法
- (void)userContentController:(WKUserContentController *)userContentController didReceiveScriptMessage:(WKScriptMessage *)message{
    NSLog(@"name:%@\\\\n body:%@\\\\n frameInfo:%@\\\\n",message.name,message.body,message.frameInfo);
    //用message.body获得JS传出的参数体
    NSDictionary * parameter = message.body;
    //JS调用OC
    if([message.name isEqualToString:@"jsToOcNoPrams"]){
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"js调用到了oc" message:@"不带参数" preferredStyle:UIAlertControllerStyleAlert];
        [alertController addAction:([UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        }])];
        [self presentViewController:alertController animated:YES completion:nil];
        
    }else if([message.name isEqualToString:@"jsToOcWithPrams"]){
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"js调用到了oc" message:parameter[@"params"] preferredStyle:UIAlertControllerStyleAlert];
        [alertController addAction:([UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        }])];
        [self presentViewController:alertController animated:YES completion:nil];
    }else if([message.name isEqualToString:@"jsToOcCallCamera"]){
        NSLog(@"----- jsToOcCallCamera------ ");
        // 使用单例模式来调用摄像头操作，模态视图present出来
        [[CameraController shareInstance] showActionSheet:self];
    }else if([message.name isEqualToString:@"jsToOcCallLocation"]){
        NSLog(@"----- jsToOcCallLocation------ ");
        // 使用单例模式来调用定位，模态视图present出来
        [[LocationController shareInstance] locate:self];
    }
}

#pragma mark -- WKNavigationDelegate
/*
 WKNavigationDelegate主要处理一些跳转、加载处理操作，WKUIDelegate主要处理JS脚本，确认框，警告框等
 */

// 页面开始加载时调用
- (void)webView:(WKWebView *)webView didStartProvisionalNavigation:(WKNavigation *)navigation {
    NSLog(@"didStartProvisionalNavigation");
}

// 页面加载失败时调用
- (void)webView:(WKWebView *)webView didFailProvisionalNavigation:(null_unspecified WKNavigation *)navigation withError:(NSError *)error {
    
    NSLog(@"#####didFailProvisionalNavigation error is %@", error);
    [self.progressView setProgress:0.0f animated:NO];
    NSLog(@" error code is %ld", error.code);
    //当由于无网络出现加载失败时，显示无网络界面
    if (error != nil && error.code == NSURLErrorNotConnectedToInternet) {
        NSLog(@" show no network UI");
        RefreshNoNetworkView *rv = [[RefreshNoNetworkView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)];
        [self.view addSubview:rv];
    }
}

// 当内容开始返回时调用
- (void)webView:(WKWebView *)webView didCommitNavigation:(WKNavigation *)navigation {
    NSLog(@"didCommitNavigation");
}

// 页面加载完成之后调用
- (void)webView:(WKWebView *)webView didFinishNavigation:(WKNavigation *)navigation {
    NSLog(@"didFinishNavigation");
}

//提交发生错误时调用
- (void)webView:(WKWebView *)webView didFailNavigation:(WKNavigation *)navigation withError:(NSError *)error{
    NSLog(@"#####didFailNavigation error is %@", error);
    [self.progressView setProgress:0.0f animated:NO];
}

// 接收到服务器跳转请求即服务重定向时之后调用
- (void)webView:(WKWebView *)webView didReceiveServerRedirectForProvisionalNavigation:(WKNavigation *)navigation {
    NSLog(@"didReceiveServerRedirectForProvisionalNavigation");
}

// 根据WebView对于即将跳转的HTTP请求头信息和相关信息来决定是否跳转
- (void)webView:(WKWebView *)webView decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {

//    NSString * urlStr = navigationAction.request.URL.absoluteString;
//    NSLog(@"发送跳转请求：%@",urlStr);
//    //自己定义的协议头
//    NSString *htmlHeadString = @"github://";
//    if([urlStr hasPrefix:htmlHeadString]){
//        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"通过截取URL调用OC" message:@"你想前往我的Github主页?" preferredStyle:UIAlertControllerStyleAlert];
//        [alertController addAction:([UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
//
//        }])];
//        [alertController addAction:([UIAlertAction actionWithTitle:@"打开" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
//            NSURL * url = [NSURL URLWithString:[urlStr stringByReplacingOccurrencesOfString:@"github://callName_?" withString:@""]];
//            [[UIApplication sharedApplication] openURL:url];
//
//        }])];
//        [self presentViewController:alertController animated:YES completion:nil];
//
//        decisionHandler(WKNavigationActionPolicyCancel);
//
//    }else{
//        decisionHandler(WKNavigationActionPolicyAllow);
//    }
    decisionHandler(WKNavigationActionPolicyAllow);
    NSLog(@"decidePolicyForNavigationAction");

}

// 根据客户端受到的服务器响应头以及response相关信息来决定是否可以跳转
- (void)webView:(WKWebView *)webView decidePolicyForNavigationResponse:(WKNavigationResponse *)navigationResponse decisionHandler:(void (^)(WKNavigationResponsePolicy))decisionHandler{
//    NSString * urlStr = navigationResponse.response.URL.absoluteString;
//    NSLog(@"当前跳转地址：%@",urlStr);
//    //允许跳转
    decisionHandler(WKNavigationResponsePolicyAllow);
    //不允许跳转
    //decisionHandler(WKNavigationResponsePolicyCancel);
    NSLog(@"decidePolicyForNavigationResponse %@", navigationResponse);
    
    
//    if (((NSHTTPURLResponse *)navigationResponse.response).statusCode == 200) {
//        decisionHandler (WKNavigationResponsePolicyAllow);
//    }else {
//        decisionHandler(WKNavigationResponsePolicyCancel);
//    }
}

//需要响应身份验证时调用 同样在block中需要传入用户身份凭证
//例如进入www.baidu.com 这种网址时会多次进行此回调，但是目前没有弄清楚机制，先直接使用父类的。
//- (void)webView:(WKWebView *)webView didReceiveAuthenticationChallenge:(NSURLAuthenticationChallenge *)challenge completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential * _Nullable credential))completionHandler{
//
//    //用户身份信息
//    NSURLCredential * newCred = [[NSURLCredential alloc] initWithUser:@"user123" password:@"123" persistence:NSURLCredentialPersistenceNone];
//    //为 challenge 的发送方提供 credential
//    [challenge.sender useCredential:newCred forAuthenticationChallenge:challenge];
//    completionHandler(NSURLSessionAuthChallengeUseCredential,newCred);
//    NSLog(@"didReceiveAuthenticationChallenge");
//
//}

//进程被终止时调用
- (void)webViewWebContentProcessDidTerminate:(WKWebView *)webView{
    NSLog(@"webViewWebContentProcessDidTerminate");
}


#pragma mark -- WKUIDelegate

/**
 *  web界面中有弹出警告框时调用
 *
 *  @param webView           实现该代理的webview
 *  @param message           警告框中的内容
 *  @param completionHandler 警告框消失调用
 */
- (void)webView:(WKWebView *)webView runJavaScriptAlertPanelWithMessage:(NSString *)message initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(void))completionHandler {
    
    NSLog(@" runJavaScriptAlertPanelWithMessage ");
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"HTML的弹出框" message:message?:@"" preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:([UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        completionHandler();
    }])];
    [self presentViewController:alertController animated:YES completion:nil];
}

// 确认框
//JavaScript调用confirm方法后回调的方法 confirm是js中的确定框，需要在block中把用户选择的情况传递进去
- (void)webView:(WKWebView *)webView runJavaScriptConfirmPanelWithMessage:(NSString *)message initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(BOOL))completionHandler{
    
    NSLog(@" runJavaScriptConfirmPanelWithMessage ");
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"" message:message?:@"" preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:([UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
        completionHandler(NO);
    }])];
    [alertController addAction:([UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        completionHandler(YES);
    }])];
    [self presentViewController:alertController animated:YES completion:nil];
}

// 输入框
//JavaScript调用prompt方法后回调的方法 prompt是js中的输入框 需要在block中把用户输入的信息传入
- (void)webView:(WKWebView *)webView runJavaScriptTextInputPanelWithPrompt:(NSString *)prompt defaultText:(NSString *)defaultText initiatedByFrame:(WKFrameInfo *)frame completionHandler:(void (^)(NSString * _Nullable))completionHandler{
    
    NSLog(@" runJavaScriptTextInputPanelWithPrompt ");
//    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:prompt message:@"" preferredStyle:UIAlertControllerStyleAlert];
//
//    [alertController addTextFieldWithConfigurationHandler:^(UITextField * _Nonnull textField) {
//        textField.text = defaultText;
//    }];
//    [alertController addAction:([UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
//        completionHandler(alertController.textFields[0].text?:@"");
//    }])];
//
//    [self presentViewController:alertController animated:YES completion:nil];
    NSLog(@"%@---%@",prompt,defaultText);
    //这里就是要返回给JS的返回值, 同步返回的，可以根据prompt来区分实现不同功能
//    NSString *deviceId = [LZKeychain getDeviceIDInKeychain];
//    NSLog(@" device ID is %@", deviceId);
    completionHandler(@"OC return str to JS according prompt use");
}

// 页面是弹出窗口 _blank 处理
- (WKWebView *)webView:(WKWebView *)webView createWebViewWithConfiguration:(WKWebViewConfiguration *)configuration forNavigationAction:(WKNavigationAction *)navigationAction windowFeatures:(WKWindowFeatures *)windowFeatures {
    
    NSLog(@" createWebViewWithConfiguration ");
    if (!navigationAction.targetFrame.isMainFrame) {
        [webView loadRequest:navigationAction.request];
    }
    return nil;
}


- (void)dealloc{
    //移除注册的js方法
    [_bridge removeHandler:@"jsCallWithOutData"];
    [_bridge removeHandler:@"jsCallWithData"];
    [_bridge removeHandler:@"jsCallGetUUID"];
    [_bridge removeHandler:@"jsCallTakePhoto"];
    [_bridge removeHandler:@"jsCallRecordVideo"];
    [_bridge removeHandler:@"jsCallScanQRCode"];
    [_bridge removeHandler:@"jsCallLocate"];
    
    //移除观察者
    [_webView removeObserver:self
                  forKeyPath:NSStringFromSelector(@selector(estimatedProgress))];
    [_webView removeObserver:self
                  forKeyPath:NSStringFromSelector(@selector(title))];
    [_webView removeObserver:self
                  forKeyPath:@"URL"];
}


@end
