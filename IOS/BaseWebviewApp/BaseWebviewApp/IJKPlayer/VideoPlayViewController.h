//
//  VideoPlayViewController.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/7/16.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "IJKMediaFramework.h"
@class IJKMediaControl;

NS_ASSUME_NONNULL_BEGIN

@interface VideoPlayViewController : UIViewController

@property(atomic,strong) NSURL *url;
@property(atomic, retain) id<IJKMediaPlayback> player;

- (id)initWithURL:(NSURL *)url;

+ (void)presentFromViewController:(UIViewController *)viewController withTitle:(NSString *)title URL:(NSURL *)url completion:(void(^)())completion;

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

@end

NS_ASSUME_NONNULL_END
