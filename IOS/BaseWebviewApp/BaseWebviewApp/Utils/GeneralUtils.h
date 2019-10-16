//
//  GeneralUtils.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/10/10.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface GeneralUtils : NSObject

//获取缓存大小
+( NSString* )getCacheSize;

//清楚缓存
+ (void)clearCache;

@end

NS_ASSUME_NONNULL_END
