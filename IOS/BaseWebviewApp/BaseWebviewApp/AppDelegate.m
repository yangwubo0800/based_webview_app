//
//  AppDelegate.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/5/31.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "AppDelegate.h"
#import "Webview/WebviewController.h"
#import "UserGuide/UserGuideView.h"
#import "Utils/Reachability.h"
#import "JPUSHService.h"
#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#import <UserNotifications/UserNotifications.h>
#endif
#import "Utils/Constant.h"
#import "BPush.h"
#import <GTSDK/GeTuiSdk.h>
#import "IFlyMSC/IFlyMSC.h"

#define MainScreen_width  [UIScreen mainScreen].bounds.size.width//宽
#define MainScreen_height [UIScreen mainScreen].bounds.size.height//高

@interface AppDelegate () <JPUSHRegisterDelegate, NSXMLParserDelegate, UNUserNotificationCenterDelegate>

@property (nonatomic, strong) Reachability *reachability;
    
//start tag
@property (nonatomic, strong) NSString *startTag;

//解析存储是否需要引导页
@property (nonatomic, strong) NSString *needGuild;

//解析存储是否需要极光推送
@property (nonatomic, strong) NSString *needJPush;

//解析存储是否需要百度云推送
@property (nonatomic, strong) NSString *needBaiduPush;

//解析存储是否需要个推推送
@property (nonatomic, strong) NSString *needGTPush;
    

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    //监听网络状态变化，不能判断那种需要登录的网络是否连接成功
    [self observerNetworkStatus];

    //window
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    //设置窗体底色，可以用来调整状态栏颜色
    [self.window setBackgroundColor:[UIColor whiteColor]];
    
    // controller
    WebviewController *vc = [WebviewController shareInstance] ;
    
    //navigate controller
    UINavigationController *nc = [[UINavigationController alloc] initWithRootViewController:vc];

    //root view
    self.window.rootViewController = nc;
    [self.window makeKeyAndVisible];
    
    // 解析配置首页地址等信息
    [self initXmlParse];
    
    // add user guide view
    if ([self.needGuild isEqualToString:@"YES"]) {
        [self addUserGuideView];
    }
    
    //根据项目是否需要极光消息推送增删此段代码，初始化必须放置在这里，不能由前端灵活调用，前端只能设置tag来过滤
    if ([self.needJPush isEqualToString:@"YES"]) {
        [self initJPush:launchOptions];
    }

    
    if([self.needBaiduPush isEqualToString:@"YES"]){
        [self initBaiduPush:launchOptions application:application];
    }
    
    if([self.needGTPush isEqualToString:@"YES"]){
        [self initGTPush];
    }
    
    //初始化科大讯飞语音识别
    [self initIFlySpeechRec];
    
    NSLog(@"============basewebview didFinishLaunchingWithOptions launchOptions=%@", launchOptions);
    // TODO: 处理冷启动时通知跳转事件，从通知消息体中获取URL，然后发送本地通知给webview, 此时webview还没有初始化。
    
    // 启动图片延时: 2秒
    [NSThread sleepForTimeInterval:2];
    
    return YES;
}

- (NSSet<NSString *> *)tags:(NSArray *)tagsList {
    NSMutableSet * tags = [[NSMutableSet alloc] init];
    [tags addObjectsFromArray:tagsList];
    //过滤掉无效的tag
    NSSet *newTags = [JPUSHService filterValidTags:tags];
    return newTags;
}

//用户引导页界面显示
-(void)addUserGuideView{
    /**
     可以在这里进行一个判断的设置，如果是app第一次启动就加载启动页，如果不是，则直接进入首页
     **/
    if (![[NSUserDefaults standardUserDefaults] boolForKey:@"everLaunched"]) {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"everLaunched"];
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"firstLaunch"];
    }else{
        [[NSUserDefaults standardUserDefaults] setBool:NO forKey:@"firstLaunch"];
    }
    
    // 这里判断是否第一次
    if ([[NSUserDefaults standardUserDefaults] boolForKey:@"firstLaunch"]) {
        
        UserGuideView *hvc = [[UserGuideView alloc]initWithFrame:CGRectMake(0, 0, MainScreen_width, MainScreen_height)];
        [self.window.rootViewController.view addSubview:hvc];
        [UIView animateWithDuration:0.25 animations:^{
            hvc.frame = CGRectMake(0, 0, MainScreen_width, MainScreen_height);
            
        }];
        
    }
}

//监听网络状态变化
-(void) observerNetworkStatus{
    // 设置网络检测的站点
    NSString *remoteHostName = @"www.apple.com";
    //注册监听
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(reachabilityChanged:) name:kReachabilityChangedNotification object:nil];
    self.reachability = [Reachability reachabilityWithHostName:remoteHostName];
    [self.reachability startNotifier];
    [self updateInterfaceWithReachability:self.reachability];
}

/*!
 * Called by Reachability whenever status changes.
 */
- (void) reachabilityChanged:(NSNotification *)note
{
    Reachability* curReach = [note object];
    NSParameterAssert([curReach isKindOfClass:[Reachability class]]);
    [self updateInterfaceWithReachability:curReach];
}

// 网络是否可用状态显示
- (void)updateInterfaceWithReachability:(Reachability *)reachability
{
    if (reachability == self.reachability)
    {
        NetworkStatus netStatus = [reachability currentReachabilityStatus];
        switch (netStatus)
        {
            case NotReachable:   {
                NSLog(@"Network Unavailable！");
                break;
            }
            case ReachableViaWWAN: {
                NSLog(@"4G/3G");
                break;
            }
            case ReachableViaWiFi: {
                NSLog(@"WiFi");
                break;
            }
        }
    }
}


//初始化个推消息推送
-(void) initGTPush {
    NSLog(@"[ GTPush ] iOS initGTPush");
    [GeTuiSdk startSdkWithAppId:kGtAppId appKey:kGtAppKey appSecret:kGtAppSecret delegate:self];
    
    float iOSVersion = [[UIDevice currentDevice].systemVersion floatValue];
    if (iOSVersion >= 10.0) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        center.delegate = self;
        [center requestAuthorizationWithOptions:(UNAuthorizationOptionBadge | UNAuthorizationOptionSound | UNAuthorizationOptionAlert) completionHandler:^(BOOL granted, NSError *_Nullable error) {
            if (!error && granted) {
                NSLog(@"[ GTPush ] iOS request authorization succeeded!");
            }
        }];
        [[UIApplication sharedApplication] registerForRemoteNotifications];
        return;
    }
    
    if (iOSVersion >= 8.0) {
        UIUserNotificationType types = (UIUserNotificationTypeAlert | UIUserNotificationTypeSound | UIUserNotificationTypeBadge);
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:types categories:nil];
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        [[UIApplication sharedApplication] registerForRemoteNotifications];
    }
}

//初始化百度云消息推送
-(void)initBaiduPush:(NSDictionary*)launchOptions application:(UIApplication *)application{
    // iOS10 下需要使用新的 API
        if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 10.0) {
    #ifdef NSFoundationVersionNumber_iOS_9_x_Max
            UNUserNotificationCenter* center = [UNUserNotificationCenter currentNotificationCenter];
            //设置代理，让消息走指定的回调
            [center setDelegate:self];
            [center requestAuthorizationWithOptions:(UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge)
                                  completionHandler:^(BOOL granted, NSError * _Nullable error) {
                                      // Enable or disable features based on authorization.
                                      if (granted) {
                                          [application registerForRemoteNotifications];
                                          NSLog(@"=====registerForRemoteNotifications");
                                      }
                                  }];
    #endif
        }
        else if ([[[UIDevice currentDevice] systemVersion] floatValue] >= 8.0) {
            UIUserNotificationType myTypes = UIUserNotificationTypeBadge | UIUserNotificationTypeSound | UIUserNotificationTypeAlert;
            
            UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:myTypes categories:nil];
            [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        }else {
            UIRemoteNotificationType myTypes = UIRemoteNotificationTypeBadge|UIRemoteNotificationTypeAlert|UIRemoteNotificationTypeSound;
            [[UIApplication sharedApplication] registerForRemoteNotificationTypes:myTypes];
        }
        #warning 上线 AppStore 时需要修改BPushMode为BPushModeProduction 需要修改Apikey为自己的Apikey
        // 在 App 启动时注册百度云推送服务，需要提供 Apikey
    [BPush registerChannel:launchOptions apiKey:BaiduPushApiKey pushMode:BPushModeDevelopment withFirstAction:@"打开" withSecondAction:@"回复" withCategory:@"test" useBehaviorTextInput:YES isDebug:YES];
        // App 是用户点击推送消息启动
        NSDictionary *userInfo = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
        if (userInfo) {
            NSLog(@"从消息启动:%@",userInfo);
            [BPush handleNotification:userInfo];
        }
}

//初始化极光推送
-(void)initJPush:(NSDictionary *)launchOptions{
    // 3.0.0及以后版本注册
    JPUSHRegisterEntity * entity = [[JPUSHRegisterEntity alloc] init];
    if (@available(iOS 12.0, *)) {
        entity.types = JPAuthorizationOptionAlert|JPAuthorizationOptionBadge|JPAuthorizationOptionSound|JPAuthorizationOptionProvidesAppNotificationSettings;
    } else {
        entity.types = JPAuthorizationOptionAlert|JPAuthorizationOptionBadge|JPAuthorizationOptionSound;
    }
    if ([[UIDevice currentDevice].systemVersion floatValue] >= 8.0) {
        //可以添加自定义categories
        //    if ([[UIDevice currentDevice].systemVersion floatValue] >= 10.0) {
        //      NSSet<UNNotificationCategory *> *categories;
        //      entity.categories = categories;
        //    }
        //    else {
        //      NSSet<UIUserNotificationCategory *> *categories;
        //      entity.categories = categories;
        //    }
    }
    [JPUSHService registerForRemoteNotificationConfig:entity delegate:self];
    
    //如不需要使用IDFA，advertisingIdentifier 可为nil
    [JPUSHService setupWithOption:launchOptions
                           appKey:JpushAppKey
                          channel:JpushChannel
                 apsForProduction:YES
            advertisingIdentifier:nil];
    //2.1.9版本新增获取registration id block接口。
    [JPUSHService registrationIDCompletionHandler:^(int resCode, NSString *registrationID) {
        if(resCode == 0){
            NSLog(@"registrationID获取成功：%@",registrationID);
        }else{
            NSLog(@"registrationID获取失败，code：%d",resCode);
        }
    }];
    
}


#pragma mark  极光消息推送代理以及适配方法实现

//注册 APNs 成功并上报 DeviceToken
- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    NSLog(@"%@", [NSString stringWithFormat:@"Device Token: %@", deviceToken]);
    //极光消息推送
    if ([self.needJPush isEqualToString:@"YES"]) {
        [JPUSHService registerDeviceToken:deviceToken];
    }
    
    //百度云消息推送
    if ([self.needBaiduPush isEqualToString:@"YES"]) {
        [BPush registerDeviceToken:deviceToken];
        [BPush bindChannelWithCompleteHandler:^(id result, NSError *error) {
               // 网络错误
               if (error) {
                   NSLog(@"bindChannelWithCompleteHandler error");
                   return ;
               }
           }];
    }
    
    if ([self.needGTPush isEqualToString:@"YES"]) {
        // [ GTSDK ]：（新版）向个推服务器注册deviceToken
        [GeTuiSdk registerDeviceTokenData:deviceToken];
    }
}

//实现注册 APNs 失败接口（可选）
- (void)application:(UIApplication *)application
didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    NSLog(@"did Fail To Register For Remote Notifications With Error: %@", error);
}

//添加处理 APNs 通知回调方法
- (void)application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSLog(@"================didReceiveRemoteNotification userInfo=%@", userInfo);
    //极光消息推送
    [JPUSHService handleRemoteNotification:userInfo];
    NSLog(@"iOS6及以下系统，收到通知:%@", [self logDic:userInfo]);

    // 百度云消息 APN收到推送的通知
    [BPush handleNotification:userInfo];
    NSLog(@"********** ios7.0之前 **********");

}

- (void)application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo
fetchCompletionHandler:
(void (^)(UIBackgroundFetchResult))completionHandler {
    NSLog(@"================didReceiveRemoteNotification fetchCompletionHandler");
    //极光消息推送
    [JPUSHService handleRemoteNotification:userInfo];
    NSLog(@"iOS7及以上系统，收到通知:%@", [self logDic:userInfo]);
    if ([[UIDevice currentDevice].systemVersion floatValue]<10.0 || application.applicationState>0) {
        NSLog(@"system version lower than IOS10  ");
    }
    
    // 百度云推送由于使用了原生注册的回调，此处逻辑不会运行
    
    
    completionHandler(UIBackgroundFetchResultNewData);
}

- (void)application:(UIApplication *)application
didReceiveLocalNotification:(UILocalNotification *)notification {
    NSLog(@"=====didReceiveLocalNotification");
    //极光消息展示
    [JPUSHService showLocalNotificationAtFront:notification identifierKey:nil];
    
}

#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#pragma mark- JPUSHRegisterDelegate
- (void)jpushNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(NSInteger))completionHandler {
    NSDictionary * userInfo = notification.request.content.userInfo;
    NSLog(@"=======Jpush willPresentNotification userInfo=%@", userInfo);
    
    completionHandler(UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionSound|UNNotificationPresentationOptionAlert); // 需要执行这个方法，选择是否提醒用户，有Badge、Sound、Alert三种类型可以设置
}

- (void)jpushNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)())completionHandler {
    
    // TODO: 处理冷 热启动时通知跳转事件，从通知消息体中获取URL，然后发送本地通知给webview
    // 冷启动时，webview 还没有初始化，所以此时还是会跳转到登录界面，先不处理这种情况
    // 目前调试阶段为直接跳转百度页面，后续url从通知内容中获取，后台在推送消息时携带跳转页面路径url.
    NSLog(@"=======Jpush didReceiveNotificationResponse");

    
    NSDictionary * userInfo = response.notification.request.content.userInfo;
    UNNotificationRequest *request = response.notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    //debug code
    NSLog(@" didReceiveNotificationResponse userInfo=%@ \n content=%@", userInfo, content);
    NSString *extraUrl = userInfo[@"jumpUrl"];
    NSLog(@" extralUrl=%@", extraUrl);
    //历史告警页面地址改为由前端设置，框架获取来跳转
    //NSString *jumpUrlKey = @"historyAlarmUrl";
    NSString *jumpUrlKey = PUSH_MESSAGE_JUMP_URL_KEY;
    NSString *jumpUrlValue = [[NSUserDefaults standardUserDefaults] objectForKey:jumpUrlKey];
    NSLog(@" jumpUrlValue=%@", jumpUrlValue);
    if (jumpUrlValue) {
        [WebviewController shareInstance].currentUrl = [NSURL URLWithString:jumpUrlValue];
        [[WebviewController shareInstance] reloadWebview];
    }

    
    NSNumber *badge = content.badge;  // 推送消息的角标
    NSString *body = content.body;    // 推送消息体
    UNNotificationSound *sound = content.sound;  // 推送消息的声音
    NSString *subtitle = content.subtitle;  // 推送消息的副标题
    NSString *title = content.title;  // 推送消息的标题
    
    if([response.notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        [JPUSHService handleRemoteNotification:userInfo];
        NSLog(@"iOS10 收到远程通知:%@", [self logDic:userInfo]);
    } else {
        // 判断为本地通知
        NSLog(@"iOS10 收到本地通知:{\nbody:%@，\ntitle:%@,\nsubtitle:%@,\nbadge：%@，\nsound：%@，\nuserInfo：%@\n}",body,title,subtitle,badge,sound,userInfo);
    }
    
    completionHandler();  // 系统要求执行这个方法
}
#endif

#ifdef __IPHONE_12_0
- (void)jpushNotificationCenter:(UNUserNotificationCenter *)center openSettingsForNotification:(UNNotification *)notification{
    NSString *title = nil;
    if (notification) {
        title = @"从通知界面直接进入应用";
    }else{
        title = @"从系统设置界面进入应用";
    }
    UIAlertView *test = [[UIAlertView alloc] initWithTitle:title
                                                   message:@"pushSetting"
                                                  delegate:self
                                         cancelButtonTitle:@"yes"
                                         otherButtonTitles:nil, nil];
    [test show];
    
}
#endif


#pragma mark - UNUserNotificationCenterDelegate
//  iOS10特性。App在前台获取通知
- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions options))completionHandler  API_AVAILABLE(ios(10.0)){

    NSLog(@"=====原生 willPresentNotification");
    UIApplication *application = [UIApplication sharedApplication];
    //百度云推送消息没有badge设置接口，需要自己处理逻辑, 调试结果似乎不行，需要后台那边发送的时候带有badge，百度没有提供接口，作罢。
//    if (application.applicationState == UIApplicationStateInactive ||
//        application.applicationState == UIApplicationStateBackground){
//            NSInteger badgeNumber = [application applicationIconBadgeNumber];
//            [application setApplicationIconBadgeNumber:badgeNumber+1];
//         }

    completionHandler(UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionSound|UNNotificationPresentationOptionAlert);
}

//  针对百度云推送，让消息在通知栏显示， iOS10特性。点击通知进入App
- (void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void(^)())completionHandler {
    
    // TODO: 百度云消息推送点击后的动作在这里处理
    NSDictionary * userInfo = response.notification.request.content.userInfo;
    UNNotificationRequest *request = response.notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    //debug code
    NSLog(@" didReceiveNotificationResponse userInfo=%@ \n content=%@", userInfo, content);
    NSLog(@"=====原生 didReceiveNotificationResponse");
    
    //历史告警页面地址改为由前端设置，框架获取来跳转
    NSString *jumpUrlKey = PUSH_MESSAGE_JUMP_URL_KEY;
    NSString *jumpUrlValue = [[NSUserDefaults standardUserDefaults] objectForKey:jumpUrlKey];
    NSLog(@" jumpUrlValue=%@", jumpUrlValue);
    if (jumpUrlValue) {
        [WebviewController shareInstance].currentUrl = [NSURL URLWithString:jumpUrlValue];
        [[WebviewController shareInstance] reloadWebview];
    }
    
    // [ GTSDK ]：将收到的APNs信息同步给个推统计
    //[GeTuiSdk handleRemoteNotification:response.notification.request.content.userInfo];
    
    completionHandler();
}


#pragma mark - GeTuiSdkDelegate
/// [ GTSDK回调 ] SDK启动成功返回cid
- (void)GeTuiSdkDidRegisterClient:(NSString *)clientId {
    NSLog(@"[ GTPush ] [GTSdk RegisterClient]:%@", clientId);
}

//创建本地通知
-(void)createNotificationWithTitle:(NSString*)title body:(NSString*)body{
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    content.title=title;
    content.body = body;
    content.sound = UNNotificationSound.defaultSound;
    UNTimeIntervalNotificationTrigger *triger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1 repeats:NO];
    UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:@"request" content:content trigger:triger];
    [[UNUserNotificationCenter currentNotificationCenter]addNotificationRequest:request withCompletionHandler:^(NSError *__nullable error){
        if (error) {
            NSLog(@"error=%@", error);
        }else{
            NSLog(@"local notifcation succeed");
        }
        
    }];
}

/// [ GTSDK回调 ] SDK收到透传消息回调
- (void)GeTuiSdkDidReceivePayloadData:(NSData *)payloadData andTaskId:(NSString *)taskId andMsgId:(NSString *)msgId andOffLine:(BOOL)offLine fromGtAppId:(NSString *)appId {
    // [ GTSDK ]：汇报个推自定义事件(反馈透传消息)
    [GeTuiSdk sendFeedbackMessage:90001 andTaskId:taskId andMsgId:msgId];
    NSString *payloadMsg = [[NSString alloc] initWithBytes:payloadData.bytes length:payloadData.length encoding:NSUTF8StringEncoding];
    NSString *msg = [NSString stringWithFormat:@"Receive Payload: %@, taskId: %@, messageId: %@ %@", payloadMsg, taskId, msgId, offLine ? @"<离线消息>" : @""];
    NSLog(@"[ GTPush ] [透传消息 GTSdk ReceivePayload]:%@", msg);
    //解析透传消息json payloadMsg  {"title":"标题7","body":"内容"}
    @try {
        NSError *error;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[payloadMsg dataUsingEncoding:NSUTF8StringEncoding] options:NSJSONReadingAllowFragments error:&error];
        NSLog(@"GTPush dict=%@", dict);
        NSArray *keys = [dict allKeys];
        
        NSString *title;
        NSString *body;
        for(int i=0; i<keys.count; i++){
            NSString *key = keys[i];
            if ([@"title" isEqualToString:key]){
                title = [dict valueForKey:key];
            }
            
            if([@"body" isEqualToString:key]){
                body = [dict valueForKey:key];
            }
        }
        //由于后台使用透传推送，当应用在前台时，系统不会回调通知，而是回调透传，为了让应用在前台时也显示通知栏，此处需要本地创建通知
        [self createNotificationWithTitle:title body:body];
    } @catch (NSException *exception) {
        NSLog(@"个推收到透传消息后创建本地通知异常");
    } @finally {
            
    }
    
}

/// [ GTSDK回调 ] SDK收到sendMessage消息回调
- (void)GeTuiSdkDidSendMessage:(NSString *)messageId result:(int)result {
    NSString *msg = [NSString stringWithFormat:@"Received sendmessage:%@ result:%d", messageId, result];
    NSLog(@"[ GTPush ] [GeTuiSdk DidSendMessage]:%@\n\n",msg);
}

/// [ GTSDK回调 ] SDK运行状态通知
- (void)GeTuiSDkDidNotifySdkState:(SdkStatus)aStatus {
    NSLog(@"[ GTPush ] [GeTuiSdk SdkStatus]:%lu\n\n", (unsigned long)aStatus);
}

/// [ GTSDK回调 ] SDK设置推送模式回调
- (void)GeTuiSdkDidSetPushMode:(BOOL)isModeOff error:(NSError *)error {
    NSLog(@"[ GTPush ] [GeTuiSdk isModeOff]:%@\n", isModeOff);
}

- (void)GeTuiSdkDidOccurError:(NSError *)error {
    NSLog(@"[ GTPush ] [GeTuiSdk GeTuiSdkDidOccurError]:%@\n\n",error.localizedDescription);
}

- (void)GeTuiSdkDidAliasAction:(NSString *)action result:(BOOL)isSuccess sequenceNum:(NSString *)aSn error:(NSError *)aError {
    /*
     参数说明
     isSuccess: YES: 操作成功 NO: 操作失败
     aError.code:
     30001：绑定别名失败，频率过快，两次调用的间隔需大于 5s
     30002：绑定别名失败，参数错误
     30003：绑定别名请求被过滤
     30004：绑定别名失败，未知异常
     30005：绑定别名时，cid 未获取到
     30006：绑定别名时，发生网络错误
     30007：别名无效
     30008：sn 无效 */
    if([action isEqual:kGtResponseBindType]) {
        NSLog(@"[ GTPush ] bind alias result sn = %@, code = %@", aSn, @(aError.code));
    }
    if([action isEqual:kGtResponseUnBindType]) {
        NSLog(@"[ GTPush ] unbind alias result sn = %@, code = %@", aSn, @(aError.code));
    }
}

- (void)GeTuiSdkDidSetTagsAction:(NSString *)sequenceNum result:(BOOL)isSuccess error:(NSError *)aError {
    /*
     参数说明
     sequenceNum: 请求的序列码
     isSuccess: 操作成功 YES, 操作失败 NO
     aError.code:
     20001：tag 数量过大（单次设置的 tag 数量不超过 100)
     20002：调用次数超限（默认一天只能成功设置一次）
     20003：标签重复
     20004：服务初始化失败
     20005：setTag 异常
     20006：tag 为空
     20007：sn 为空
     20008：离线，还未登陆成功
     20009：该 appid 已经在黑名单列表（请联系技术支持处理）
     20010：已存 tag 数目超限
     20011：tag 内容格式不正确
     */
    NSLog(@"[ GTPush ] GeTuiSdkDidSetTagAction sequenceNum:%@ isSuccess:%@ error: %@", sequenceNum, @(isSuccess), aError);
}



// log NSSet with UTF8
// if not ,log will be \Uxxx
- (NSString *)logDic:(NSDictionary *)dic {
    if (![dic count]) {
        return nil;
    }
    NSString *tempStr1 =
    [[dic description] stringByReplacingOccurrencesOfString:@"\\u"
                                                 withString:@"\\U"];
    NSString *tempStr2 =
    [tempStr1 stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""];
    NSString *tempStr3 =
    [[@"\"" stringByAppendingString:tempStr2] stringByAppendingString:@"\""];
    NSData *tempData = [tempStr3 dataUsingEncoding:NSUTF8StringEncoding];
    NSString *str =
    [NSPropertyListSerialization propertyListFromData:tempData
                                     mutabilityOption:NSPropertyListImmutable
                                               format:NULL
                                     errorDescription:NULL];
    return str;
}

    
    // 初始化xml解析器
-(void) initXmlParse{
    NSString *configPath = [[NSBundle mainBundle] pathForResource:@"app_config.xml" ofType:nil inDirectory:@"Resource"];
    NSLog(@"initXmlParse configPath=%@", configPath);
    NSData *data = [[NSData alloc] initWithContentsOfFile:configPath];
    NSXMLParser *parser = [[NSXMLParser alloc] initWithData:data];
    NSLog(@"#####initXmlParse parser is %p",parser);
    parser.delegate = self;
    [parser parse];
}

// 初始化科大讯飞语音识别
-(void) initIFlySpeechRec{
    //Set log level
    [IFlySetting setLogFile:LVL_ALL];
    
    //Set whether to output log messages in Xcode console
    [IFlySetting showLogcat:YES];

    //Set the local storage path of SDK
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    NSString *cachePath = [paths objectAtIndex:0];
    [IFlySetting setLogFilePath:cachePath];
    
    //Set APPID
    NSString *initString = [[NSString alloc] initWithFormat:@"appid=%@",IFLY_APPID_VALUE];
    
    //Configure and initialize iflytek services.(This interface must been invoked in application:didFinishLaunchingWithOptions:)
    [IFlySpeechUtility createUtility:initString];
}
/**
 开始解析
 */
- (void)parserDidStartDocument:(NSXMLParser *)parser{
    //这里只是开始,貌似不用做什么
    NSLog(@"#####parserDidStartDocument");
}
    
    /**
     开始一个新标签,这个时候应该创建对应的模型对象或者准备为模型的属性赋值.
     @param parser 解析器
     @param elementName 标签元素名字
     @param attributeDict 标签的属性
     */
- (void)parser:(NSXMLParser *)parser didStartElement:(NSString *)elementName namespaceURI:(nullable NSString *)namespaceURI qualifiedName:(nullable NSString *)qName attributes:(NSDictionary<NSString *, NSString *> *)attributeDict{
    self.startTag = elementName;
    NSLog(@"#####didStartElement self.startTag is %@", self.startTag);
}
    
    
    
    /**
     解析到标签中间的文字 标签中的文字不是一次性能读完的,可能会分几次调用这个方法,所以创建一个可变字符串保存起来.
     
     @param parser 解析器
     @param string 文字
     */
- (void)parser:(NSXMLParser *)parser foundCharacters:(NSString *)string{
    NSLog(@"#####foundCharacters string is %@", string);
    if([self.startTag isEqualToString:@"first_page_url"]){
        // controller
        WebviewController *vc = [WebviewController shareInstance] ;
        vc.parsedFirstPageUrl = string;
    }
    
    if([self.startTag isEqualToString:@"need_guide_page"]){
        self.needGuild = string;
    }
    
    if([self.startTag isEqualToString:@"guide_page_count"]){
        NSString *pageCount = string;
        self.pageCount = [pageCount intValue];
        //限制引导页在2和4之间
        if (self.pageCount < 2) {
            self.pageCount = 2;
        } else if (self.pageCount > 4){
            self.pageCount = 4;
        }
    }
    
    if([self.startTag isEqualToString:@"need_jpush"]){
        self.needJPush = string;
    }
    
    if([self.startTag isEqualToString:@"need_baiduPush"]){
        self.needBaiduPush = string;
    }
    
    if([self.startTag isEqualToString:@"need_GTPush"]){
        self.needGTPush = string;
    }
}
    
    
    
    /**
     解析到一个元素结束的地方.
     
     @param parser 解析器
     @param elementName 元素名字
     */
- (void)parser:(NSXMLParser *)parser didEndElement:(NSString *)elementName namespaceURI:(nullable NSString *)namespaceURI qualifiedName:(nullable NSString *)qName{
    self.startTag = nil;
    NSLog(@"didEndElement");
}
    
    
    /**
     结束解析
     */
- (void)parserDidEndDocument:(NSXMLParser *)parser{
    //所有标签解析完毕,打印数组看看是否转换成功.
    NSLog(@"parserDidEndDocument");
}



- (void)dealloc
{
    //移除网络监听
    [self.reachability stopNotifier];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:kReachabilityChangedNotification object:nil];
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    NSLog(@"applicationDidEnterBackground set icon badge nubmer to 0");
    [[UIApplication sharedApplication] setApplicationIconBadgeNumber:0];
    //告诉极光服务器，应用角标清理
    [JPUSHService setBadge:0];
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    NSLog(@"applicationWillEnterForeground set icon badge nubmer to 0");
    [application setApplicationIconBadgeNumber:0];
    [application cancelAllLocalNotifications];
    //告诉极光服务器，应用角标清理
    [JPUSHService setBadge:0];
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}


- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}


//在AppDelegate.m中添加转屏的代理方法
- (UIInterfaceOrientationMask)application:(UIApplication *)application supportedInterfaceOrientationsForWindow:(nullable UIWindow *)window{
    
//    if (self.allowRotation == YES) {
//        //横屏
//        return UIInterfaceOrientationMaskAll;
//
//    }else{
//        //竖屏
//        return UIInterfaceOrientationMaskPortrait;
//
//    }
    
    if ([self.orientationMode isEqualToString:@"0"]) {
        return UIInterfaceOrientationMaskAll;
    }else if ([self.orientationMode isEqualToString:@"1"]){
        return UIInterfaceOrientationMaskLandscape;
    }else {
        return UIInterfaceOrientationMaskPortrait;
    }
    
}

@end
