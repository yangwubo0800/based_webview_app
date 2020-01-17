//
//  HikRealplayViewController.m
//  HikVideoPlayer
//
//  Created by wangchuanyin on 09/06/2018.
//  Copyright (c) 2018 wangchuanyin. All rights reserved.
//

@import AVKit;
@import AVFoundation;
@import Photos;

#import <Toast/Toast.h>
#import <HikVideoPlayer/HVPError.h>
#import <HikVideoPlayer/HVPPlayer.h>
#import "HikRealplayViewController.h"
#import "../AppDelegate.h"
#import "../Utils/UIDevice+TFDevice.h"
#import "AFNetworking.h"

//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)

static NSTimeInterval const kToastDuration = 1;

//提供给外部的实例
static HikRealplayViewController *instance = nil;

@interface HikRealplayViewController () <HVPPlayerDelegate>
//@property (weak, nonatomic) IBOutlet UIActivityIndicatorView *indicatorView;
@property (weak, nonatomic) IBOutlet UIButton                     *playButton;
@property (weak, nonatomic) IBOutlet UITextField                 *realplayTextField;
@property (weak, nonatomic) IBOutlet UIButton *recordButton;
@property (nonatomic, strong) HVPPlayer                            *player;
@property (nonatomic, assign) BOOL                                  isPlaying;
@property (nonatomic, assign) BOOL                                  isRecording;
@property (nonatomic, copy) NSString                             *recordPath;
@property (nonatomic, strong) UIActivityIndicatorView *indicator;
@property (weak, nonatomic) IBOutlet UIBarButtonItem *videoTitle;

@end

@implementation HikRealplayViewController

- (IBAction)onClickDone:(id)sender
{
    [self.presentingViewController dismissViewControllerAnimated:YES completion:nil];
}

+(void)presentFromViewController:(UIViewController *)viewController URL:(NSString *)url videoTitle:(NSString *)title{
    
    [viewController presentViewController:[[HikRealplayViewController alloc] initWithURL:url videoTitle:title] animated:YES completion:nil];
}


- (instancetype)initWithURL:(NSString *)url  videoTitle:(NSString *)title{
    self = [self initWithNibName:@"HikRealplayViewController" bundle:nil];
    if (self) {
        self.url = url;
        self.title = title;
    }
    NSLog(@"initWithURL url is %@", url);
    
    //将每次的示例赋值给对象
    instance = self;
    return self;
}


+(HikRealplayViewController*)getInstance{
    return instance;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // 注册前后台切换通知
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applicationWillResignActive) name:UIApplicationWillResignActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(applicationDidBecomeActive) name:UIApplicationDidBecomeActiveNotification object:nil];
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    //允许转成横屏
    appDelegate.allowRotation = YES;
    //调用横屏代码
    [UIDevice switchNewOrientation:UIInterfaceOrientationLandscapeRight];
    NSLog(@"=====viewDidLoad switch UIInterfaceOrientationLandscapeRight");
    
    self.indicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
    CGRect frame = self.indicator.frame;
    frame.origin = CGPointMake((SCREEN_WIDTH - self.indicator.frame.size.width)/2, SCREEN_HEIGHT/2);
    self.indicator.frame = frame;
    self.indicator.hidesWhenStopped = YES;
    [self.indicator startAnimating];
    [self.view addSubview:self.indicator];
    // 设置视频标题
    [self.videoTitle setTitle:self.title ];
    // 实际开发中需要根据平台获取
    //_realplayTextField.text = @"rtsp://175.6.40.71:554/openUrl/RgTuTok";
    
    //使用异步任务来实现耗时操作
    //    [self performSelectorInBackground:@selector(previewVideo) withObject:nil];
}

//-(void)previewVideo{
//    //debug
//    UIButton *debugBt = [UIButton alloc];
//    [debugBt setTitle:@"开始预览" forState:UIControlStateNormal];
//    [self startRealPlay:debugBt];
//    NSLog(@"=====viewDidLoad startRealPlay");
//}

- (void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    NSLog(@"-----viewDidAppear");
    //debug
    UIButton *debugBt = [UIButton alloc];
    [debugBt setTitle:@"开始预览" forState:UIControlStateNormal];
    [self startRealPlay:debugBt];
    NSLog(@"=====viewDidLoad startRealPlay");
}


- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    
    //允许转成竖屏
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    appDelegate.allowRotation = NO;//关闭横屏仅允许竖屏
    //切换到竖屏
    [UIDevice switchNewOrientation:UIInterfaceOrientationPortrait];
    NSLog(@"=====viewDidDisappear switch UIInterfaceOrientationPortrait");
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
    return UIInterfaceOrientationIsLandscape(toInterfaceOrientation);
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskLandscape;
}


- (void)dealloc {
    if (_isRecording) {
        //如果在录像，先关闭录像
        [self recordVideo:_recordButton];
    }
    // 退出当前页面，需要停止播放
    if (_isPlaying) {
        [_player stopPlay:nil];
    }
}

// 添加视频配置信息
-(void) addVideoConfigInfo{
    self.videoConfigInfo = [[UILabel alloc] init];
    //[self.videoConfigInfo setText:@"视频配置信息：\n朝辞白帝彩云间，\n 千里江陵一日还。\n 两岸猿声啼不住，\n 轻舟已过万重山。\n"];
    [self.videoConfigInfo setTextColor:[UIColor whiteColor]];
    [self.videoConfigInfo setBackgroundColor:[[UIColor blackColor]colorWithAlphaComponent:0.5f]];
    self.videoConfigInfo.numberOfLines = 0;
    self.videoConfigInfo.lineBreakMode = NSLineBreakByTruncatingTail;
    CGSize maxLabelSize = CGSizeMake(200, 9999);
    CGSize expectSize = [self.videoConfigInfo sizeThatFits:maxLabelSize];
    self.videoConfigInfo.frame = CGRectMake(20, 50, expectSize.width, expectSize.height);
    [self.view addSubview:self.videoConfigInfo];
    
    //是否需要关闭
    //    [NSTimer scheduledTimerWithTimeInterval:3 target:self selector:@selector(updateVideoInfo:) userInfo:nil repeats:YES];
}

- (IBAction)startRealPlay:(UIButton *)sender {
    if ([sender.currentTitle isEqualToString:@"开始预览"]) {
        //        if (_realplayTextField.text.length == 0) {
        //            [self.view makeToast:@"请输入预览URL" duration:kToastDuration position:CSToastPositionCenter];
        //            return;
        //        }
        
        NSString *videoUrl = self.url;
        // 开始加载动画
        //[self.indicatorView startAnimating];
        // 为避免卡顿，开启预览可以放到子线程中，在应用中灵活处理
        NSLog(@"start real play....");
        if (![self.player startRealPlay:videoUrl]) {
            NSLog(@"start real play failed");
            //[self.indicatorView stopAnimating];
            [self.indicator stopAnimating];
        }
        NSLog(@"start real play succeed");
        return;
    }
    if (_isRecording) {
        //如果在录像，先关闭录像
        [self recordVideo:_recordButton];
    }
    [_player stopPlay:nil];
    _isPlaying = NO;
    [sender setTitle:@"开始预览" forState:UIControlStateNormal];
}

#pragma mark - 相关操作
// 抓图
- (IBAction)capturePicture:(UIButton *)sender {
    if (!_isPlaying) {
        [self.view makeToast:@"未播放视频，不能抓图" duration:kToastDuration position:CSToastPositionCenter];
        return;
    }
    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (status == PHAuthorizationStatusDenied) {
                [self.view makeToast:@"无保存图片到相册的权限，不能抓图" duration:kToastDuration position:CSToastPositionCenter];
            }
            else {
                [self capture];
            }
        });
    }];
}

- (void)capture {
    if (!_isPlaying) {
        return;
    }
    // 生成图片路径
    NSString *documentDirectorie = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES).firstObject;
    NSString *filePath = [documentDirectorie stringByAppendingFormat:@"/%.f.jpg", [NSDate date].timeIntervalSince1970];
    NSError *error;
    if (![_player capturePicture:filePath error:&error]) {
        NSString *message = [NSString stringWithFormat:@"抓图失败，错误码是 0x%08lx", error.code];
        [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
    }
    else {
        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
            [PHAssetChangeRequest creationRequestForAssetFromImageAtFileURL:[NSURL URLWithString:filePath]];
        } completionHandler:^(BOOL success, NSError * _Nullable error) {
            NSString *message;
            if (success) {
                message = @"抓图成功，并保存到系统相册";
            }
            else {
                message = @"保存到系统相册失败";
            }
            [[NSFileManager defaultManager] removeItemAtPath:filePath error:nil];
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
            });
        }];
    }
}

// 录像
- (IBAction)record:(UIButton *)sender {
    if (!_isPlaying) {
        [self.view makeToast:@"未播放视频，不能录像" duration:kToastDuration position:CSToastPositionCenter];
        return;
    }
    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
        if (status == PHAuthorizationStatusDenied) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.view makeToast:@"无保存录像到相册的权限，不能录像" duration:1 position:CSToastPositionCenter];
            });
        }
        else {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self recordVideo:sender];
            });
        }
    }];
}

- (void)recordVideo:(UIButton *)sender {
    if (!_isPlaying) {
        return;
    }
    NSError *error;
    // 开始录像
    if ([sender.currentTitle isEqualToString:@"开始录像"]) {
        // 生成图片路径
        NSString *documentDirectorie = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES).firstObject;
        NSString *filePath = [documentDirectorie stringByAppendingFormat:@"/%.f.mp4", [NSDate date].timeIntervalSince1970];
        _recordPath = [filePath copy];
        if ([_player startRecord:filePath error:&error]) {
            _isRecording = YES;
            [sender setTitle:@"停止录像" forState:UIControlStateNormal];
        }
        else {
            NSString *message = [NSString stringWithFormat:@"开始录像失败，错误码是 0x%08lx", error.code];
            [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
        }
        return;
    }
    if (!_isRecording) {
        return;
    }
    // 停止录像
    if ([_player stopRecord:&error]) {
        _isRecording = NO;
        [sender setTitle:@"开始录像" forState:UIControlStateNormal];
        //可在自定义recordPath路径下取录像文件
        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
            [PHAssetChangeRequest creationRequestForAssetFromVideoAtFileURL:[NSURL URLWithString:_recordPath]];
        } completionHandler:^(BOOL success, NSError * _Nullable error) {
            NSString *message;
            if (success) {
                message = @"录像成功，并保存到系统相册";
            }
            else {
                message = @"保存到系统相册失败";
            }
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
            });
        }];
    }
    else {
        NSString *message = [NSString stringWithFormat:@"停止录像失败，错误码是 0x%08lx", error.code];
        [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
    }
}

// 声音
- (IBAction)sound:(UIButton *)sender {
    if (!_isPlaying) {
        [self.view makeToast:@"未播放视频，不能静音" duration:kToastDuration position:CSToastPositionCenter];
        return;
    }
    NSError *error;
    if ([sender.currentTitle isEqualToString:@"静音"]) {
        if ([_player enableSound:NO error:&error]) {
            [sender setTitle:@"开启声音" forState:UIControlStateNormal];
        }
        else {
            NSString *message = [NSString stringWithFormat:@"静音失败，错误码是 0x%08lx", error.code];
            [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
        }
        return;
    }
    // 开启声音
    if ([_player enableSound:YES error:&error]) {
        [sender setTitle:@"静音" forState:UIControlStateNormal];
    }
    else {
        NSString *message = [NSString stringWithFormat:@"开启声音失败，错误码是 0x%08lx", error.code];
        [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
    }
}

#pragma mark - HVPPlayerDelegate

- (void)player:(HVPPlayer *)player playStatus:(HVPPlayStatus)playStatus errorCode:(HVPErrorCode)errorCode {
    dispatch_async(dispatch_get_main_queue(), ^{
        // 如果有加载动画，结束加载动画
//        if (self.indicatorView.isAnimating) {
//            [self.indicatorView stopAnimating];
//        }
        if (self.indicator.isAnimating) {
            [self.indicator stopAnimating];
        }
        _isPlaying = NO;
        NSString *message;
        // 预览时，没有HVPPlayStatusFinish状态，该状态表明录像片段已播放完
        if (playStatus == HVPPlayStatusSuccess) {
            _isPlaying = YES;
            [_playButton setTitle:@"停止预览" forState:UIControlStateNormal];
            // 默认开启声音
            [_player enableSound:YES error:nil];
            //添加视频配置显示控件
            [self addVideoConfigInfo];
            //[self performSelector:@selector(addVideoConfigInfo) withObject:nil afterDelay:3];
            //获取后台数据进行填充
            if (self.videoInfoUrl) {
                AFHTTPSessionManager *manager =[AFHTTPSessionManager manager];
                NSLog(@"-------start get updateVideoCfgInfo .....");
                // parameters 参数字典
                [manager GET:self.videoInfoUrl parameters:nil progress:^(NSProgress * _Nonnull downloadProgress) {
                    //进度
                    NSLog(@"downloadProgress is %lld", downloadProgress.completedUnitCount);
                } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
                    // responseObject:请求成功返回的响应结果（AFN内部已经把响应体转换为OC对象，通常是字典或数组)
                    NSLog(@" success responseObject is %@", responseObject);
                    //TODO: 解析后台返回数据，并更新
                    NSDictionary *dictResponse =  responseObject;
                    NSDictionary *data = [dictResponse objectForKey:@"data"];
                    NSString *showInfo=@"";
                    NSArray *keys = [data allKeys];
                    for (int i=0; i< [keys count]; i++) {
                        NSString *key = keys[i];
                        NSString *value = [data objectForKey:key];
                        showInfo = [NSString stringWithFormat:@"%@：%@\n%@", key, value, showInfo];
                    }
                    //TODO: 更新
                    //NSString* info = [StringUtils UIUtilsFomateJsonWithDictionary:responseObject];
                    NSLog(@" setVideoConfigInfo showInfo=%@", showInfo);
                    [self.videoConfigInfo setText:showInfo];
                    [self.videoConfigInfo setBackgroundColor:[[UIColor blackColor]colorWithAlphaComponent:0.5f]];
                    [self.videoConfigInfo setTextColor:[UIColor whiteColor]];
                    self.videoConfigInfo.numberOfLines = 0;
                    self.videoConfigInfo.lineBreakMode = NSLineBreakByTruncatingTail;
                    CGSize maxLabelSize = CGSizeMake(200, 9999);
                    CGSize expectSize = [self.videoConfigInfo sizeThatFits:maxLabelSize];
                    self.videoConfigInfo.frame = CGRectMake(20, 50, expectSize.width, expectSize.height);
                    
                } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
                    // error 错误信息
                    NSLog(@" failure response is %@", task.response);
                    NSLog(@" failure error is %@", error.description);
                }];
            }else{
                NSLog(@"The video config info url is nil");
            }
        }
        else if (playStatus == HVPPlayStatusFailure) {
            if (errorCode == HVPErrorCodeURLInvalid) {
                message = @"URL输入错误请检查URL或者URL已失效请更换URL";
            }
            else {
                message = [NSString stringWithFormat:@"开启预览失败, 错误码是 : 0x%08lx", errorCode];
            }
        }
        else if (playStatus == HVPPlayStatusException) {
            // 预览过程中出现异常, 可能是取流中断，可能是其他原因导致的，具体根据错误码进行区分
            // 做一些提示操作
            message = [NSString stringWithFormat:@"播放异常, 错误码是 : 0x%08lx", errorCode];
            if (_isRecording) {
                //如果在录像，先关闭录像
                [self recordVideo:_recordButton];
            }
            // 关闭播放
            [_player stopPlay:nil];
        }
        if (message) {
            [self.view makeToast:message duration:kToastDuration position:CSToastPositionCenter];
        }
    });
}

#pragma mark - Private Method

- (void)applicationWillResignActive {
    if (_isRecording) {
        [self recordVideo:_recordButton];
    }
    _isPlaying = NO;
    [_player stopPlay:nil];
}

- (void)applicationDidBecomeActive {
    if ([_playButton.currentTitle isEqualToString:@"停止预览"]) {
//        [self.indicatorView startAnimating];
//        if (![_player startRealPlay:_realplayTextField.text]) {
//            [self.indicatorView stopAnimating];
//        }
        [self.indicator startAnimating];
        if (![_player startRealPlay:_realplayTextField.text]) {
            [self.indicator stopAnimating];
        }
    }
}

#pragma mark - Override Method

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [_realplayTextField resignFirstResponder];
}

#pragma mark - Setter or Getter

- (HVPPlayer *)player {
    if (!_player) {
        // 创建player
        _player = [[HVPPlayer alloc] initWithPlayView:self.playView];
        // 或者 _player = [HVPPlayer playerWithPlayView:self.playView];
        // 设置delegate
        _player.delegate = self;
    }
    return _player;
}


@end
