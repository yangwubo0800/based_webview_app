//
//  AppDelegate.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/5/31.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>

//JPush param
static NSString *appKey = @"xxxxxxxxxxxxxxxxxxx";
static NSString *channel = @"Publish channel";
static BOOL isProduction = FALSE;


@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

/**
 * 是否允许转向
 */
@property(nonatomic,assign)BOOL allowRotation;

//解析引导页个数配置
@property(nonatomic, assign)NSInteger pageCount;

@end

