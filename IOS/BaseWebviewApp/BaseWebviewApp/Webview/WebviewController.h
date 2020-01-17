//
//  WebviewController.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/6.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "../WebViewJavascriptBridge/WebViewJavascriptBridge.h"
#import "../dsbridge/DWKWebView.h"

NS_ASSUME_NONNULL_BEGIN

@interface WebviewController : UIViewController

// 开源框架https://github.com/marcuswestin/WebViewJavascriptBridge
@property WebViewJavascriptBridge* bridge;


// 单例模式，提供给某些类需要使用桥来回调JS方法
+(WebviewController *)shareInstance;


//提给为网络恢复时重新加载页面使用
-(void)reloadWebview;

// 提供直播功能
- (void)ijkLivePlay:(NSString *)livePath withTitle:(NSString *)title;

// 视频播放功能
- (void)ijkVideoPlay:(NSString*)videoPath;

//记录当前加载的URL
@property(nonatomic, strong) NSURL *currentUrl;
    
//解析xml中获得首页地址
@property(nonatomic, strong) NSString *parsedFirstPageUrl;

//webview
@property(nonatomic, strong) DWKWebView *webView;

//记录扫码调用者名称
@property(nonatomic, strong) NSString *scanCallerName;
//记录定位调用者名称
@property(nonatomic, strong) NSString *locateCallerName;
    

@end

NS_ASSUME_NONNULL_END
