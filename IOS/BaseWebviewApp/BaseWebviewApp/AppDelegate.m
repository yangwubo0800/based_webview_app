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

#define MainScreen_width  [UIScreen mainScreen].bounds.size.width//宽
#define MainScreen_height [UIScreen mainScreen].bounds.size.height//高

@interface AppDelegate () <JPUSHRegisterDelegate, NSXMLParserDelegate>

@property (nonatomic, strong) Reachability *reachability;
    
//start tag
@property (nonatomic, strong) NSString *startTag;

//解析存储是否需要引导页
@property (nonatomic, strong) NSString *needGuild;

    

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    //监听网络状态变化
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
    
    // TODO:根据项目是否需要极光消息推送增删此段代码，初始化必须放置在这里，不能由前端灵活调用，前端只能设置tag来过滤消息
    //极光消息推送初始化
    [self initJPush:launchOptions];
    
    NSLog(@"============basewebview didFinishLaunchingWithOptions launchOptions=%@", launchOptions);
    // TODO: 处理冷启动时通知跳转事件，从通知消息体中获取URL，然后发送本地通知给webview, 此时webview还没有初始化。
//    NSDictionary *userInfo = launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey];
//    if (userInfo) {
//        NSLog(@"remote notification info is %@", userInfo);
//        [WebviewController shareInstance].currentUrl = [NSURL URLWithString:@"http://www.baidu.com"];
//        [[WebviewController shareInstance] reloadWebview];
//    }
    
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
                           appKey:appKey
                          channel:channel
                 apsForProduction:isProduction
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
    [JPUSHService registerDeviceToken:deviceToken];
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
    [JPUSHService handleRemoteNotification:userInfo];
    NSLog(@"iOS6及以下系统，收到通知:%@", [self logDic:userInfo]);
}

- (void)application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo
fetchCompletionHandler:
(void (^)(UIBackgroundFetchResult))completionHandler {
    NSLog(@"================didReceiveRemoteNotification fetchCompletionHandler");
    [JPUSHService handleRemoteNotification:userInfo];
    NSLog(@"iOS7及以上系统，收到通知:%@", [self logDic:userInfo]);
    
    if ([[UIDevice currentDevice].systemVersion floatValue]<10.0 || application.applicationState>0) {
        NSLog(@"system version lower than IOS10  ");
    }
    
    completionHandler(UIBackgroundFetchResultNewData);
}

- (void)application:(UIApplication *)application
didReceiveLocalNotification:(UILocalNotification *)notification {
    [JPUSHService showLocalNotificationAtFront:notification identifierKey:nil];
}

#ifdef NSFoundationVersionNumber_iOS_9_x_Max
#pragma mark- JPUSHRegisterDelegate
- (void)jpushNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(NSInteger))completionHandler {
    NSLog(@"=======willPresentNotification");
    NSDictionary * userInfo = notification.request.content.userInfo;
    
    UNNotificationRequest *request = notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    
    NSNumber *badge = content.badge;  // 推送消息的角标
    NSString *body = content.body;    // 推送消息体
    UNNotificationSound *sound = content.sound;  // 推送消息的声音
    NSString *subtitle = content.subtitle;  // 推送消息的副标题
    NSString *title = content.title;  // 推送消息的标题
    
    if([notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        [JPUSHService handleRemoteNotification:userInfo];
        NSLog(@"iOS10 前台收到远程通知:%@", [self logDic:userInfo]);
    }else {
        // 判断为本地通知
        NSLog(@"iOS10 前台收到本地通知:{\nbody:%@，\ntitle:%@,\nsubtitle:%@,\nbadge：%@，\nsound：%@，\nuserInfo：%@\n}",body,title,subtitle,badge,sound,userInfo);
    }
    completionHandler(UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionSound|UNNotificationPresentationOptionAlert); // 需要执行这个方法，选择是否提醒用户，有Badge、Sound、Alert三种类型可以设置
}

- (void)jpushNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)())completionHandler {
    
    // TODO: 处理冷 热启动时通知跳转事件，从通知消息体中获取URL，然后发送本地通知给webview
    // 冷启动时，webview 还没有初始化，所以此时还是会跳转到登录界面，先不处理这种情况
    // 目前调试阶段为直接跳转百度页面，后续url从通知内容中获取，后台在推送消息时携带跳转页面路径url.
    NSLog(@"=======didReceiveNotificationResponse");
    [WebviewController shareInstance].currentUrl = [NSURL URLWithString:@"http://www.baidu.com"];
    [[WebviewController shareInstance] reloadWebview];
    
    NSDictionary * userInfo = response.notification.request.content.userInfo;
    UNNotificationRequest *request = response.notification.request; // 收到推送的请求
    UNNotificationContent *content = request.content; // 收到推送的消息内容
    //debug code
    NSLog(@" didReceiveNotificationResponse userInfo=%@ \n content=%@", userInfo, content);
    NSString *extraUrl = userInfo[@"jumpUrl"];
    NSLog(@" extralUrl=%@", extraUrl);
    
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
    
    if (self.allowRotation == YES) {
        //横屏
        return UIInterfaceOrientationMaskLandscape;
        
    }else{
        //竖屏
        return UIInterfaceOrientationMaskPortrait;
        
    }
    
}

@end
