/*
 * Copyright (C) 2013-2015 Bilibili
 * Copyright (C) 2013-2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "IJKMoviePlayerViewController.h"
#import "IJKMediaControl.h"
#import "../AppDelegate.h"
#import "../Utils/UIDevice+TFDevice.h"
//#import "IJKCommon.h"
//#import "IJKDemoHistory.h"
#import "AFNetworking.h"
#import "../Utils/StringUtils.h"

//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)



//提供给外部的实例
static IJKVideoViewController *instance = nil;

@implementation IJKVideoViewController

- (void)dealloc
{
}

+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url completion:(void (^)())completion {
//    IJKDemoHistoryItem *historyItem = [[IJKDemoHistoryItem alloc] init];
//    
//    historyItem.title = title;
//    historyItem.url = url;
//    [[IJKDemoHistory instance] add:historyItem];
    
    [viewController presentViewController:[[IJKVideoViewController alloc] initWithURL:url withTitle:title] animated:YES completion:completion];
}

- (instancetype)initWithURL:(NSURL *)url withTitle:(NSString *)title{
    self = [self initWithNibName:@"IJKMoviePlayerViewController" bundle:nil];
    if (self) {
        self.url = url;
        self.videoTitle = title;
    }
    //将每次的示例赋值给对象
    instance = self;
    
    return self;
}

+(IJKVideoViewController*)getInstance{
    return instance;
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

#define EXPECTED_IJKPLAYER_VERSION (1 << 16) & 0xFF) | 
- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.

//    [[UIApplication sharedApplication] setStatusBarHidden:YES];
//    [[UIApplication sharedApplication] setStatusBarOrientation:UIInterfaceOrientationLandscapeLeft animated:NO];
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    //允许转成横屏
    //appDelegate.allowRotation = YES;
    appDelegate.orientationMode = @"1";
    //调用横屏代码
    [UIDevice switchNewOrientation:UIInterfaceOrientationLandscapeRight];
    NSLog(@"=====viewDidLoad switch UIInterfaceOrientationLandscapeRight");
#ifdef DEBUG
    [IJKFFMoviePlayerController setLogReport:YES];
    [IJKFFMoviePlayerController setLogLevel:k_IJK_LOG_DEBUG];
#else
    [IJKFFMoviePlayerController setLogReport:NO];
    [IJKFFMoviePlayerController setLogLevel:k_IJK_LOG_INFO];
#endif

    [IJKFFMoviePlayerController checkIfFFmpegVersionMatch:YES];
    // [IJKFFMoviePlayerController checkIfPlayerVersionMatch:YES major:1 minor:0 micro:0];

//    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
//
//    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
//    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
//    self.player.view.frame = self.view.bounds;
//    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
//    self.player.shouldAutoplay = YES;

    self.view.autoresizesSubviews = YES;
    [self.view addSubview:self.player.view];
    [self.view addSubview:self.mediaControl];

//    self.mediaControl.delegatePlayer = self.player;
    //使用异步任务来实现耗时操作
    [self performSelectorInBackground:@selector(initIjkPlayer) withObject:nil];
    
    NSLog(@"=========================add subview for indicator");
    //TODO: add indicator and tip label on player.view
    self.indicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
    CGRect frame = self.indicator.frame;
    //frame.origin = CGPointMake((playerViewWidth - self.indicator.frame.size.width)/2, playerViewHeight/2);
    //landscape play
    frame.origin = CGPointMake((SCREEN_WIDTH - self.indicator.frame.size.width)/2, SCREEN_HEIGHT/2);
    self.indicator.frame = frame;
    self.indicator.hidesWhenStopped = YES;
    [self.indicator startAnimating];
    [self.view addSubview:self.indicator];
    
    self.label = [[UILabel alloc] init];
    self.label.frame = CGRectMake((SCREEN_WIDTH-100)/2, SCREEN_HEIGHT/2 + 20, 100, 50);
    [self.label setText:@"视频加载中..."];
    //[label setBackgroundColor:[UIColor blueColor]];
    [self.view addSubview:self.label];
    
    // TODO: live play hide bottom panel
    self.mediaControl.bottomPanel.hidden = YES;
    [self.mediaControl.videoTitleItem setTitle:self.videoTitle];
    
    
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    NSLog(@"-----viewWillAppear");
//    [self installMovieNotificationObservers];
//
//    [self.player prepareToPlay];
}

- (void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    NSLog(@"-----viewDidAppear");
    
//    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
//    
//    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
//    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
//    self.player.view.frame = self.view.bounds;
//    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
//    self.player.shouldAutoplay = YES;
//    
//    self.mediaControl.delegatePlayer = self.player;
//    
//    [self installMovieNotificationObservers];
//    
//    [self.player prepareToPlay];
    
}

-(void) initIjkPlayer{
    NSLog(@"#######initIjkPlayer");
    // TODO: 为了规避第一次初始化播放器耗时过长，而且页面显示被阻塞的问题，将播放器初始化放入异步线程中
    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    self.player.view.frame = self.view.bounds;
    //全屏播放设置，高和宽都缩放
    self.player.scalingMode = IJKMPMovieScalingModeFill;
    self.player.shouldAutoplay = YES;
    
    [self.view addSubview:self.player.view];
    [self.view addSubview:self.mediaControl];
    
    
    self.mediaControl.delegatePlayer = self.player;
    
    [self installMovieNotificationObservers];
    [self.player prepareToPlay];
}

// 添加视频配置信息
-(void) addVideoConfigInfo{
    self.videoConfigInfo = [[UILabel alloc] init];
//    [self.videoConfigInfo setText:@"视频配置信息：\n朝辞白帝彩云间，\n 千里江陵一日还。\n 两岸猿声啼不住，\n 轻舟已过万重山。\n"];
    [self.videoConfigInfo setTextColor:[UIColor whiteColor]];
    self.videoConfigInfo.numberOfLines = 0;
    self.videoConfigInfo.lineBreakMode = NSLineBreakByTruncatingTail;
    CGSize maxLabelSize = CGSizeMake(200, 9999);
    CGSize expectSize = [self.videoConfigInfo sizeThatFits:maxLabelSize];
    self.videoConfigInfo.frame = CGRectMake(20, 50, expectSize.width, expectSize.height);
    [self.view addSubview:self.videoConfigInfo];
    //是否需要关闭
//    [NSTimer scheduledTimerWithTimeInterval:3 target:self selector:@selector(updateVideoInfo:) userInfo:nil repeats:YES];
}


- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    
    [self.player shutdown];
    [self removeMovieNotificationObservers];
    //允许转成竖屏
    AppDelegate * appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
    //appDelegate.allowRotation = NO;//关闭横屏仅允许竖屏
    appDelegate.orientationMode = @"2";
    //切换到竖屏
    [UIDevice switchNewOrientation:UIInterfaceOrientationPortrait];
    //销毁定时器
    if (self.timer) {
        [self.timer invalidate];
        NSLog(@"viewDidDisappear invalidate timer");
    }
    if (self.videoInfoUrl) {
        self.videoInfoUrl = nil;
        NSLog(@"viewDidDisappear clean video info url");
    }
    NSLog(@"=====viewDidDisappear switch UIInterfaceOrientationPortrait");
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation{
    return UIInterfaceOrientationIsLandscape(toInterfaceOrientation);
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
    return UIInterfaceOrientationMaskLandscape;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark IBAction

- (IBAction)onClickMediaControl:(id)sender
{
    [self.mediaControl showAndFade];
}

- (IBAction)onClickOverlay:(id)sender
{
    [self.mediaControl hide];
}

- (IBAction)onClickDone:(id)sender
{
    [self.presentingViewController dismissViewControllerAnimated:YES completion:nil];
}

- (IBAction)onClickHUD:(UIBarButtonItem *)sender
{
//    if ([self.player isKindOfClass:[IJKFFMoviePlayerController class]]) {
//        IJKFFMoviePlayerController *player = self.player;
//        player.shouldShowHudView = !player.shouldShowHudView;
//
//        sender.title = (player.shouldShowHudView ? @"HUD On" : @"HUD Off");
//    }
}

- (IBAction)onClickPlay:(id)sender
{
    [self.player play];
    [self.mediaControl refreshMediaControl];
}

- (IBAction)onClickPause:(id)sender
{
    [self.player pause];
    [self.mediaControl refreshMediaControl];
}

- (IBAction)didSliderTouchDown
{
    [self.mediaControl beginDragMediaSlider];
}

- (IBAction)didSliderTouchCancel
{
    [self.mediaControl endDragMediaSlider];
}

- (IBAction)didSliderTouchUpOutside
{
    [self.mediaControl endDragMediaSlider];
}

- (IBAction)didSliderTouchUpInside
{
    self.player.currentPlaybackTime = self.mediaControl.mediaProgressSlider.value;
    [self.mediaControl endDragMediaSlider];
}

- (IBAction)didSliderValueChanged
{
    [self.mediaControl continueDragMediaSlider];
}

- (void)loadStateDidChange:(NSNotification*)notification
{
    //    MPMovieLoadStateUnknown        = 0,
    //    MPMovieLoadStatePlayable       = 1 << 0,
    //    MPMovieLoadStatePlaythroughOK  = 1 << 1, // Playback will be automatically started in this state when shouldAutoplay is YES
    //    MPMovieLoadStateStalled        = 1 << 2, // Playback will be automatically paused in this state, if started

    IJKMPMovieLoadState loadState = _player.loadState;

    if ((loadState & IJKMPMovieLoadStatePlaythroughOK) != 0) {
        NSLog(@"loadStateDidChange: IJKMPMovieLoadStatePlaythroughOK: %d\n", (int)loadState);
    } else if ((loadState & IJKMPMovieLoadStateStalled) != 0) {
        NSLog(@"loadStateDidChange: IJKMPMovieLoadStateStalled: %d\n", (int)loadState);
    } else {
        NSLog(@"loadStateDidChange: ???: %d\n", (int)loadState);
    }
}

- (void)moviePlayBackDidFinish:(NSNotification*)notification
{
    //    MPMovieFinishReasonPlaybackEnded,
    //    MPMovieFinishReasonPlaybackError,
    //    MPMovieFinishReasonUserExited
    int reason = [[[notification userInfo] valueForKey:IJKMPMoviePlayerPlaybackDidFinishReasonUserInfoKey] intValue];

    switch (reason)
    {
        case IJKMPMovieFinishReasonPlaybackEnded:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonPlaybackEnded: %d\n", reason);
            break;

        case IJKMPMovieFinishReasonUserExited:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonUserExited: %d\n", reason);
            break;

        case IJKMPMovieFinishReasonPlaybackError:
            NSLog(@"playbackStateDidChange: IJKMPMovieFinishReasonPlaybackError: %d\n", reason);
            break;

        default:
            NSLog(@"playbackPlayBackDidFinish: ???: %d\n", reason);
            break;
    }
}

- (void)mediaIsPreparedToPlayDidChange:(NSNotification*)notification
{
    NSLog(@"mediaIsPreparedToPlayDidChange\n");
    //self.indicator.stopAnimating;
    [self.indicator stopAnimating];
    self.label.hidden = YES;
    //添加视频配置显示控件
    [self addVideoConfigInfo];
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

- (void)moviePlayBackStateDidChange:(NSNotification*)notification
{
    //    MPMoviePlaybackStateStopped,
    //    MPMoviePlaybackStatePlaying,
    //    MPMoviePlaybackStatePaused,
    //    MPMoviePlaybackStateInterrupted,
    //    MPMoviePlaybackStateSeekingForward,
    //    MPMoviePlaybackStateSeekingBackward

    switch (_player.playbackState)
    {
        case IJKMPMoviePlaybackStateStopped: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: stoped", (int)_player.playbackState);
            break;
        }
        case IJKMPMoviePlaybackStatePlaying: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: playing", (int)_player.playbackState);
            break;
        }
        case IJKMPMoviePlaybackStatePaused: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: paused", (int)_player.playbackState);
            break;
        }
        case IJKMPMoviePlaybackStateInterrupted: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: interrupted", (int)_player.playbackState);
            break;
        }
        case IJKMPMoviePlaybackStateSeekingForward:
        case IJKMPMoviePlaybackStateSeekingBackward: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: seeking", (int)_player.playbackState);
            break;
        }
        default: {
            NSLog(@"IJKMPMoviePlayBackStateDidChange %d: unknown", (int)_player.playbackState);
            break;
        }
    }
}

#pragma mark Install Movie Notifications

/* Register observers for the various movie object notifications. */
-(void)installMovieNotificationObservers
{
	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(loadStateDidChange:)
                                                 name:IJKMPMoviePlayerLoadStateDidChangeNotification
                                               object:_player];

	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlayBackDidFinish:)
                                                 name:IJKMPMoviePlayerPlaybackDidFinishNotification
                                               object:_player];

	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(mediaIsPreparedToPlayDidChange:)
                                                 name:IJKMPMediaPlaybackIsPreparedToPlayDidChangeNotification
                                               object:_player];

	[[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(moviePlayBackStateDidChange:)
                                                 name:IJKMPMoviePlayerPlaybackStateDidChangeNotification
                                               object:_player];
}

#pragma mark Remove Movie Notification Handlers

/* Remove the movie notification observers from the movie object. */
-(void)removeMovieNotificationObservers
{
    [[NSNotificationCenter defaultCenter]removeObserver:self name:IJKMPMoviePlayerLoadStateDidChangeNotification object:_player];
    [[NSNotificationCenter defaultCenter]removeObserver:self name:IJKMPMoviePlayerPlaybackDidFinishNotification object:_player];
    [[NSNotificationCenter defaultCenter]removeObserver:self name:IJKMPMediaPlaybackIsPreparedToPlayDidChangeNotification object:_player];
    [[NSNotificationCenter defaultCenter]removeObserver:self name:IJKMPMoviePlayerPlaybackStateDidChangeNotification object:_player];
}

@end
