//
//  ISRDataHelper.h
//  BaseWebviewApp
//
//  Created by hongbo ni on 2021/1/12.
//  Copyright © 2021 hongbo ni. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface ISRDataHelper : NSObject

/**
 parse JSON data
 **/
+ (NSString *)stringFromJson:(NSString*)params;//


/**
 parse JSON data for cloud grammar recognition
 **/
+ (NSString *)stringFromABNFJson:(NSString*)params;


@end

NS_ASSUME_NONNULL_END
