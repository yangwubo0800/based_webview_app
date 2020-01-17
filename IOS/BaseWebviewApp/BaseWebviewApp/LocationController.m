//
//  LocationController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/13.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "LocationController.h"
#import "Webview/WebviewController.h"
#import "Utils/StringUtils.h"

static LocationController *instance;

//确保一次请求定位只返回一次
static Boolean hasPostLoaction;

@implementation LocationController

//singlton
+(LocationController *)shareInstance{
    if (instance == nil) {
        instance = [[LocationController alloc] init];
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


// 权限提示方法
-(void)alertShowPermissionTip{
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"提示" message:@"您还未开启定位服务，是否需要开启？" preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *cancel = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction * _Nonnull action) {
    }];
    UIAlertAction *confirm = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        NSURL *setingsURL = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
        [[UIApplication sharedApplication]openURL:setingsURL];
    }];
    
    [alert addAction:cancel];
    [alert addAction:confirm];
    [self.uiViewController.navigationController presentViewController:alert animated:YES completion:nil];
}


-(void)showLocate:(UIViewController *)vc{
    
    NSString *result = [[NSString alloc] initWithFormat:@"address:%@\n latitude:%@\n longitude:%@",self.address, self.latitude, self.longitude];
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"locate result" message:result preferredStyle:UIAlertControllerStyleAlert];
    
    UIAlertAction *action = [UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action){
    }];
    [alert addAction:action];
    [vc.navigationController presentViewController:alert animated:YES completion:nil];
}


- (void)locate:(UIViewController *)vc{
    //pass param
    self.uiViewController = vc;
    NSLog(@"locate vc is %p", self.uiViewController);
    if ([CLLocationManager locationServicesEnabled]) {//监测权限设置
        NSLog(@"location service is enable");
        self.locationManager = [[CLLocationManager alloc]init];
        self.locationManager.delegate = self;//设置代理
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBest;//设置精度
        self.locationManager.distanceFilter = 1000.0f;//距离过滤
        [self.locationManager requestAlwaysAuthorization];//位置权限申请
        CLAuthorizationStatus locateStatus = [CLLocationManager authorizationStatus];
        NSLog(@" locateStatus is %d", locateStatus);
        //用户拒绝过，让用户先开启权限
        if (locateStatus == kCLAuthorizationStatusDenied) {
            [self alertShowPermissionTip];
            return;
        } else{
            // TODO: 当用户第一次操作，系统弹框半天不操作，停留一段时间后会有异常，因为定位已经开始。
            [self.locationManager startUpdatingLocation];//开始定位
            NSLog(@"really start update location...");
            hasPostLoaction = false;
            //设置10秒超时
            [self performSelector:@selector(processLoacteTimeout) withObject:nil afterDelay:10];
        }
    } else{
        NSLog(@"location service is unable");
        [self alertShowPermissionTip];
    }
}

-(void)processLoacteTimeout{
    //调用JS方法将定位信息回传给前端
    WebviewController *wc = [WebviewController shareInstance];
    NSString *locateCallerName = wc.locateCallerName;
    NSDictionary *dict = @{@"name":locateCallerName,
                           @"address":self.address,
                           @"latitude":self.latitude,
                           @"longitude":self.longitude,
                           @"type":@"timeout"
                           };
    //[[[WebviewController shareInstance] bridge] callHandler:@"feedBackLocateResult" data:dict];
    //修改为使用webview直接发送emit时间给前端
    NSString *emitName = @"location";
    NSString *locateJsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:dict];
    NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
    js = [js stringByAppendingString:@"','"];
    js = [js stringByAppendingString:locateJsonStr];
    js = [js stringByAppendingString:@"')"];
    NSLog(@" js is :%@", js);
    [[wc webView] evaluateJavaScript:js completionHandler:nil];
    
    [self.locationManager stopUpdatingLocation];//停止定位
}

#pragma mark location代理
-(void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error {
    NSLog(@"locate didFailWithError...");
    [self alertShowPermissionTip];
}

-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations {
    NSLog(@"locate didUpdateLocations...");
    
    //取消超时
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(processLoacteTimeout) object:nil];
    [self.locationManager stopUpdatingLocation];//停止定位
    //地理反编码
    CLLocation *currentLocation = [locations lastObject];
    CLGeocoder *geoCoder = [[CLGeocoder alloc]init];
    //当系统设置为其他语言时，可利用此方法获得中文地理名称
    NSMutableArray *userDefaultLanguages = [[NSUserDefaults standardUserDefaults]objectForKey:@"AppleLanguages"];
    // 强制 成 简体中文
    [[NSUserDefaults standardUserDefaults] setObject:[NSArray arrayWithObjects:@"zh-hans", nil]forKey:@"AppleLanguages"];
    
    //地理信息解码也是个异步过程
    [geoCoder reverseGeocodeLocation:currentLocation completionHandler:^(NSArray<CLPlacemark *> * _Nullable placemarks, NSError * _Nullable error) {
        if (placemarks.count > 0) {
            CLPlacemark *placeMark = placemarks[0];
            NSString *wholeAddress = [[NSString alloc] initWithFormat:@"%@%@%@%@%@%@%@",
                                      placeMark.country,
                                      placeMark.administrativeArea,
                                      ((placeMark.subAdministrativeArea == nil)?@"":placeMark.subAdministrativeArea),
                                      placeMark.locality,
                                      ((placeMark.subLocality == nil)?@"":placeMark.subLocality),
                                      placeMark.thoroughfare,
                                      ((placeMark.subThoroughfare == nil)?@"":placeMark.subThoroughfare)];
            NSLog(@"whole address is %@",wholeAddress);
            
            //获取经纬度
            double latitude = placeMark.location.coordinate.latitude;
            double longitude = placeMark.location.coordinate.longitude;
            
            self.address = wholeAddress;
            self.latitude = [[NSString alloc] initWithFormat:@"%f", latitude];
            self.longitude = [[NSString alloc] initWithFormat:@"%f", longitude];
            NSLog(@" latitude is %@", self.latitude);
            NSLog(@" longitude is %@", self.longitude);
            
            // 显示定位结果信息
            //[self showLocate:self.uiViewController];
            // TODO: 调用JS方法将定位信息回传给前端
            WebviewController *wc = [WebviewController shareInstance];
            NSString *locateCallerName = wc.locateCallerName;
            NSDictionary *dict = @{@"name":locateCallerName,
                                   @"address":self.address,
                                   @"latitude":self.latitude,
                                   @"longitude":self.longitude,
                                   @"type":@"normal"
                                   };
            //[[[WebviewController shareInstance] bridge] callHandler:@"feedBackLocateResult" data:dict];
            //修改为使用webview直接发送emit时间给前端
            NSLog(@"location hasPostLoaction=%hhu", hasPostLoaction);
            if (!hasPostLoaction) {
                NSString *emitName = @"location";
                NSString *locateJsonStr = [StringUtils UIUtilsFomateJsonWithDictionary:dict];
                NSString *js = [@"MOBILE_API.emit('" stringByAppendingString:emitName];
                js = [js stringByAppendingString:@"','"];
                js = [js stringByAppendingString:locateJsonStr];
                js = [js stringByAppendingString:@"')"];
                NSLog(@" js is :%@", js);
                [[wc webView] evaluateJavaScript:js completionHandler:nil];
                hasPostLoaction = true;
            }

            
            NSString *city = placeMark.locality;
            if (!city) {
                self.currentCity = @"⟳定位获取失败,点击重试";
                // TODO: 有可能是直辖市，需要获取 administrativeArea 字段
            } else {
                self.currentCity = placeMark.locality ;//获取当前城市
                
            }
            
        } else if (error == nil && placemarks.count == 0 ) {
            
        } else if (error) {
            self.currentCity = @"⟳定位获取失败,点击重试";
        }
        // 还原Device 的语言
        [[NSUserDefaults
          standardUserDefaults] setObject:userDefaultLanguages
         forKey:@"AppleLanguages"];
    }];
    
    NSLog(@"locate self.currentCity is %@", self.currentCity);
    
  
}


@end
