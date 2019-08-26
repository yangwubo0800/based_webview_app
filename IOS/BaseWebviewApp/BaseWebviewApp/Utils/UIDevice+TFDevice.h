//
//  UIDevice+TFDevice.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/8/21.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <UIKit/UIKit.h>
@interface UIDevice (TFDevice)
/**
 * @interfaceOrientation 输入要强制转屏的方向
 */
+ (void)switchNewOrientation:(UIInterfaceOrientation)interfaceOrientation;
@end
