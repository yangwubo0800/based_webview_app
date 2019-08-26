//
//  RealHtmlController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/8/21.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "RealHtmlController.h"
#import <WebKit/WebKit.h>
#import "../UserGuide/RefreshNoNetworkView.h"
#import "../AppDelegate.h"
#import "../Utils/UIDevice+TFDevice.h"

//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)

#define iPhoneX ([UIScreen instancesRespondToSelector:@selector(currentMode)] ? CGSizeEqualToSize(CGSizeMake(1125, 2436), [[UIScreen mainScreen] currentMode].size) : NO)

//状态栏和导航栏高度
//通常 状态栏+导航栏=20+44=64
//iPhone X 状态栏+导航栏=44+44=88
#define StatusBarAndNavigationBarHeight (iPhoneX ? 88.f : 64.f)


@interface RealHtmlController () <WKNavigationDelegate, WKUIDelegate, WKScriptMessageHandler, UINavigationControllerDelegate>

//webview
@property(nonatomic, strong) WKWebView *webView;
//进度条
@property(nonatomic, strong) UIProgressView *progressView;

//记录当前加载的URL
@property(nonatomic, strong) NSURL *currentUrl;



@end

@implementation RealHtmlController


-(instancetype)initWithUrl:(NSURL *)url{
    //不要重复alloc，否则会导致前面的对象被后面的覆盖，造成莫名其妙的现象
    //self = [[RealHtmlController alloc] init];
    NSLog(@"#####start html controller self is %p",self);
    if (self) {
         self.currentUrl = url;
    }else{
        NSLog(@"initWithUrl fail");
    }
    
    return self;
}

-(void)viewWillAppear:(BOOL)animated{
    [super viewWillAppear:animated];
    //禁用侧滑手势方法
    self.navigationController.interactivePopGestureRecognizer.enabled = NO;
}

-(void)viewWillDisappear:(BOOL)animated{
    [super viewWillDisappear:animated];
    //禁用侧滑手势方法
    self.navigationController.interactivePopGestureRecognizer.enabled = YES;
    //点击导航栏返回按钮的时候调用，所以Push出的控制器最好禁用侧滑手势：
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    appDelegate.allowRotation = NO;//关闭横屏仅允许竖屏
    //切换到竖屏
    [UIDevice switchNewOrientation:UIInterfaceOrientationPortrait];
    //[self.navigationController popViewControllerAnimated:YES];
}


- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    
    //检查网络
    //[checkNetwork checkNetworkCanUse];
    
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    //允许转成横屏
    appDelegate.allowRotation = YES;
    //调用横屏代码
    [UIDevice switchNewOrientation:UIInterfaceOrientationLandscapeRight];
    
    self.navigationItem.title = @"实时画面";
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


//- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
//    NSLog(@"=====shouldAutorotateToInterfaceOrientation toInterfaceOrientation is %ld", (long)toInterfaceOrientation);
//    return UIInterfaceOrientationIsLandscape(toInterfaceOrientation);
//}
//
//- (UIInterfaceOrientationMask)supportedInterfaceOrientations
//{
//    NSLog(@"=====supportedInterfaceOrientations");
//    return UIInterfaceOrientationMaskLandscape;
//}



#pragma mark getter

- (UIProgressView *)progressView
{
    NSLog(@"real html _progressView is %p", _progressView);
    if (!_progressView){
        //获取导航栏的高度
        CGFloat navHeight = self.navigationController.navigationBar.frame.size.height;
        NSLog(@"real html 导航栏高度：%f",navHeight);
        //设置进度条位置和大小,view 的起始位置逻辑在导航栏下面
        _progressView = [[UIProgressView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, 2)];
        //颜色与安卓平台保持一致
        _progressView.tintColor = [UIColor blueColor];
        _progressView.trackTintColor = [UIColor clearColor];
    }
    return _progressView;
}


- (WKWebView *)webView{
    NSLog(@"real html _webView is %p", _webView);
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
        
        //横屏显示需要将长和宽对调
        CGFloat navigationHeight =  self.navigationController.navigationBar.frame.size.height;
        NSLog(@"#####navigationHeight=%f", navigationHeight);
        _webView = [[WKWebView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT) configuration:config];
        // UI代理
        _webView.UIDelegate = self;
        // 导航代理
        _webView.navigationDelegate = self;
        
       
//        // 2.创建URL
//        //debug http://m.electro.hnaccloud.com
//        NSString *path = firstPagePath;
//        //转义字符或字符串中含有中文， 都可能导致url=nil,需要处理
//        NSString * urlStr = [path stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSURL *url = self.currentUrl;
        if (url != nil) {
            self.currentUrl = url;
            NSLog(@" first time create webview current url is %@", self.currentUrl);
        }
        // 3.创建Request
        NSURLRequest *request =[NSURLRequest requestWithURL:url];
        // 4.加载网页
        [_webView loadRequest:request];
        
    }
    return _webView;
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
    NSLog(@"====== real html controller dealloc");
    //移除观察者
    [_webView removeObserver:self
                  forKeyPath:NSStringFromSelector(@selector(estimatedProgress))];
    [_webView removeObserver:self
                  forKeyPath:NSStringFromSelector(@selector(title))];
    [_webView removeObserver:self
                  forKeyPath:@"URL"];
}

@end
