//
//  AppDelegate.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/5/31.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>

//JPush param
static NSString *appKey = @"xxxxxxxxxxx";
static NSString *channel = @"Publish channel";
static BOOL isProduction = YES;
//
////baidu push param
//static NSString *BaiduPushApiKey = @"xxxxxxxxxxx";


@interface AppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;

/**
 * 是否允许转向
 */
@property(nonatomic,assign)BOOL allowRotation;

/**
 * 屏幕旋转模式: 0 跟随屏幕旋转， 1 只允许横屏， 2 只允许竖屏
 */
@property(nonatomic, strong) NSString* orientationMode;


//解析引导页个数配置
@property(nonatomic, assign)NSInteger pageCount;



@end

