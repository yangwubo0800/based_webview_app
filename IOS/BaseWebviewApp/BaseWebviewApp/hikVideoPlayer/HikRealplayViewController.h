//
//  HikRealplayViewController.h
//  HikVideoPlayer
//
//  Created by wangchuanyin on 09/06/2018.
//  Copyright (c) 2018 wangchuanyin. All rights reserved.
//

@import UIKit;

@interface HikRealplayViewController : UIViewController

//视频播放地址
@property(atomic,strong) NSString *url;
//视频标题
@property(atomic,strong) NSString *title;
// 显示视频配置信息
@property (strong, nonatomic) UILabel *videoConfigInfo;
// 定时更新配置信息的定时器
@property(retain, nonatomic) NSTimer* timer;
// 更新视频配置信息的后台地址
@property(copy, nonatomic) NSString* videoInfoUrl;

@property (weak, nonatomic) IBOutlet UIView *playView;


//呈现界面
+(void)presentFromViewController:(UIViewController *)viewController URL:(NSString *)url videoTitle:(NSString*)title;


+(HikRealplayViewController*)getInstance;

@end
