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

#import <UIKit/UIKit.h>
//#import <IJKMediaFramework/IJKMediaFramework.h>
#import "IJKMediaFramework.h"
@class IJKMediaControl;

@interface IJKVideoViewController : UIViewController

@property(atomic,strong) NSURL *url;
@property(atomic,strong) NSString *videoTitle;
@property(atomic, retain) id<IJKMediaPlayback> player;

- (id)initWithURL:(NSURL *)url withTitle:(NSString *)title;

+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url completion:(void(^)())completion;


+(IJKVideoViewController*)getInstance;

- (IBAction)onClickMediaControl:(id)sender;
- (IBAction)onClickOverlay:(id)sender;
- (IBAction)onClickDone:(id)sender;
- (IBAction)onClickPlay:(id)sender;
- (IBAction)onClickPause:(id)sender;

- (IBAction)didSliderTouchDown;
- (IBAction)didSliderTouchCancel;
- (IBAction)didSliderTouchUpOutside;
- (IBAction)didSliderTouchUpInside;
- (IBAction)didSliderValueChanged;

@property(nonatomic,strong) IBOutlet IJKMediaControl *mediaControl;

@property (strong, nonatomic) UIActivityIndicatorView *indicator;
@property (strong, nonatomic) UILabel *label;
// 显示视频配置信息
@property (strong, nonatomic) UILabel *videoConfigInfo;
// 定时更新配置信息的定时器
@property(retain, nonatomic) NSTimer* timer;
// 更新视频配置信息的后台地址
@property(copy, nonatomic) NSString* videoInfoUrl;

@end
