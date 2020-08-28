//
//  VideoPlayViewController.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/7/16.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "VideoPlayViewController.h"
#import "IJKMediaControl.h"
#import "../AppDelegate.h"
#import "../Utils/UIDevice+TFDevice.h"
//竖屏幕宽高
#define SCREEN_WIDTH ([UIScreen mainScreen].bounds.size.width)
#define SCREEN_HEIGHT ([UIScreen mainScreen].bounds.size.height)

@interface VideoPlayViewController ()

@end

@implementation VideoPlayViewController

- (void)dealloc
{
}

+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url completion:(void (^)())completion {
    //    IJKDemoHistoryItem *historyItem = [[IJKDemoHistoryItem alloc] init];
    //
    //    historyItem.title = title;
    //    historyItem.url = url;
    //    [[IJKDemoHistory instance] add:historyItem];
    
    [viewController presentViewController:[[VideoPlayViewController alloc] initWithURL:url] animated:YES completion:completion];
}

- (instancetype)initWithURL:(NSURL *)url {
    self = [self initWithNibName:@"VideoPlayViewController" bundle:nil];
    if (self) {
        self.url = url;
    }
    return self;
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
//    // TODO: 为了适配各个手机的屏幕全屏显示，需要将view的frame进行全屏大小适配
////    CGRect fullScreen = CGRectMake(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
//    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
//    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
//    self.player.view.frame = self.view.bounds;
//    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
//    self.player.shouldAutoplay = YES;
    
    
    self.view.autoresizesSubviews = YES;
//    [self.view addSubview:self.player.view];
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
    
    
    
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    
//    [self installMovieNotificationObservers];
//
//    [self.player prepareToPlay];
}

- (void)viewDidAppear:(BOOL)animated{
    [super viewDidAppear:animated];
    NSLog(@"-----viewDidAppear");
    
    // TODO: 为了规避第一次初始化播放器耗时过长，而且页面显示被阻塞的问题，将播放器初始化动作移置此处
//    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
//    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
//    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
//    self.player.view.frame = self.view.bounds;
//    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
//    self.player.shouldAutoplay = YES;
//
//    self.view.autoresizesSubviews = YES;
//    [self.view addSubview:self.player.view];
//    [self.view addSubview:self.mediaControl];
//
//    self.mediaControl.delegatePlayer = self.player;
//
//    [self installMovieNotificationObservers];
//    [self.player prepareToPlay];
    
}

-(void) initIjkPlayer{
    NSLog(@"#######initIjkPlayer");
    // TODO: 为了规避第一次初始化播放器耗时过长，而且页面显示被阻塞的问题，将播放器初始化放入异步线程中
    IJKFFOptions *options = [IJKFFOptions optionsByDefault];
    self.player = [[IJKFFMoviePlayerController alloc] initWithContentURL:self.url withOptions:options];
    self.player.view.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    self.player.view.frame = self.view.bounds;
    self.player.scalingMode = IJKMPMovieScalingModeAspectFit;
    self.player.shouldAutoplay = YES;
    
    [self.view addSubview:self.player.view];
    [self.view addSubview:self.mediaControl];
    
    self.mediaControl.delegatePlayer = self.player;
    
    [self installMovieNotificationObservers];
    [self.player prepareToPlay];
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
    self.indicator.stopAnimating;
    self.label.hidden = YES;
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
