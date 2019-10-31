//
//  ImagePreviewViewController.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/10/28.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface ImagePreviewViewController : UIViewController

// 单例模式
+(ImagePreviewViewController *)shareInstance;


//减少内存消耗，使用弱引用
@property (weak, nonatomic) IBOutlet UIImageView *imgView;

//预览图片方法
- (void)show:(NSString *)imageUrl;

@end

NS_ASSUME_NONNULL_END
