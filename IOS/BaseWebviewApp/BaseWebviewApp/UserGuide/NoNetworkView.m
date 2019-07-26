//
//  NoNetworkView.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/27.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "NoNetworkView.h"
#import "../Webview/WebviewController.h"
#import "../Utils/checkNetwork.h"

#define MainScreen_width  [UIScreen mainScreen].bounds.size.width//宽
#define MainScreen_height [UIScreen mainScreen].bounds.size.height//高

@implementation NoNetworkView



-(instancetype)initWithFrame:(CGRect)frame
{
    NSLog(@"no network view init ");
    self = [super initWithFrame:frame];
    if (self) {
        UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake((MainScreen_width-200)/2, MainScreen_height/2, 200, 50)];
        label.text = @"请检查网络连接情况";
        label.textColor = [UIColor greenColor];

        UIButton *button = [[UIButton alloc] initWithFrame:CGRectMake((MainScreen_width-200)/2, MainScreen_height/2 + 50, 200, 50)];
        [button setTitle:@"重新加载" forState:UIControlStateNormal];
        [button setBackgroundColor:[UIColor blackColor]];
        [button addTarget:self action:@selector(handleReload) forControlEvents:UIControlEventTouchUpInside];
        
        [self setBackgroundColor:[UIColor grayColor]];
        [self addSubview:label];
        [self addSubview:button];
    }

    return self;

}



-(void)handleReload{
    //if network available, then hide it
    // if not , hide and show it again ?
    
//    if ([checkNetwork checkNetworkCanUse]) {
//        self.hidden = YES;
//        WebviewController *vc = [WebviewController shareInstance];
//        [vc reloadWebview];
//    } else {
//        NSLog(@" network still not available");
//        self.hidden = YES;
//        self.hidden = NO;
//    }
    
    self.hidden = YES;
    WebviewController *vc = [WebviewController shareInstance];
    [vc reloadWebview];

    
}

@end
