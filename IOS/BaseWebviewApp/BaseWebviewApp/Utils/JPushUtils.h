//
//  JPushUtils.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/7/10.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface JPushUtils : NSObject

// 使用逗号分隔来一次设置多个tag
+(void)setJPushTags:(NSString *) tags;

+(void)cleanJPushTag;

@end

NS_ASSUME_NONNULL_END
