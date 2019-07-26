//
//  checkNetwork.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/25.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "checkNetwork.h"
#import "../UserGuide/RefreshNoNetworkView.h"
#import "../Webview/WebviewController.h"

#define kAppleUrlTocheckWifi @"http://captive.apple.com"
//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)


@implementation checkNetwork

//检测网络是否可以使用
+(BOOL)checkNetworkCanUse{
    
    // 1.将网址初始化成一个OC字符串对象
    NSString *urlStr = kAppleUrlTocheckWifi;
    // 如果网址中存在中文,进行URLEncode
    NSString *newUrlStr = [urlStr stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    // 2.构建网络URL对象, NSURL
    NSURL *url = [NSURL URLWithString:newUrlStr];
    // 3.创建网络请求， 只有3秒钟的超时时间
    NSURLRequest *request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestReloadIgnoringLocalCacheData timeoutInterval:3];
    // 创建同步链接
    NSURLResponse *response = nil;
    NSError *error = nil;
    NSData *data = [NSURLConnection sendSynchronousRequest:request returningResponse:&response error:&error];
    
    NSString* result1 = [[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
    //解析html页面
    NSString *str = [self flattenHTML:result1];
    //除掉换行符
    NSString *nstr = [str stringByReplacingOccurrencesOfString:@"\n" withString:@""];
    
    if ([nstr isEqualToString:@"SuccessSuccess"])
    {
         NSLog(@"可以上网了");
        //   [PronetwayGeneralHandle shareHandle].NetworkCanUse = YES;
        return YES;
    }else {
         NSLog(@"未联网");
        //[self showNetworkStatus:@"未联网"];
        //   [PronetwayGeneralHandle shareHandle].NetworkCanUse = NO;
        // TODO: 提供网络请求接口给前端，如果没有网络，则显示无网络界面，但是此处是异步返回，需要注意, 慎用！
        RefreshNoNetworkView *rv = [[RefreshNoNetworkView alloc] initWithFrame:CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT)];
        [[[WebviewController shareInstance] view] addSubview:rv];
        NSLog(@" add no network view to webview controller");
        return NO;
    }
    
}


//过滤后台返回字符串中的标签
+ (NSString *)flattenHTML:(NSString *)html {
    
    NSScanner *theScanner;
    NSString *text = nil;
    
    theScanner = [NSScanner scannerWithString:html];
    
    while ([theScanner isAtEnd] == NO) {
        // find start of tag
        [theScanner scanUpToString:@"<" intoString:NULL] ;
        // find end of tag
        [theScanner scanUpToString:@">" intoString:&text] ;
        // replace the found tag with a space
        //(you can filter multi-spaces out later if you wish)
        html = [html stringByReplacingOccurrencesOfString:
                [NSString stringWithFormat:@"%@>", text]
                                               withString:@""];
    }
    return html;
}


@end
