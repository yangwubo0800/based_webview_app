//
//  LocationController.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/13.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface LocationController : NSObject <CLLocationManagerDelegate>

//设置manager
@property (nonatomic, strong) CLLocationManager *locationManager;

@property (nonatomic, strong) NSString *currentCity;

//定位地址全称
@property (nonatomic, strong) NSString *address;
//纬度
@property (nonatomic, strong) NSString *latitude;
//经度
@property (nonatomic, strong) NSString *longitude;


// 传递过来的视图控制器
@property(nonatomic, strong) UIViewController *uiViewController;


// 单例模式
+(LocationController *)shareInstance;

//显示定位结果
-(void)showLocate:(UIViewController *)vc;

//启动定位
- (void)locate:(UIViewController *)vc;



@end

NS_ASSUME_NONNULL_END
