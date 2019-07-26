//
//  StringUtils.m
//  BaseWebviewApp
//
//  Created by hongbo ni on 2019/6/27.
//  Copyright © 2019 hongbo ni. All rights reserved.
//

#import "StringUtils.h"

@implementation StringUtils

+ (BOOL)isBlankString:(NSString *)string{
    
    if (string == nil) {
        
        return YES;
        
    }
    
    if (string == NULL) {
        
        return YES;
        
    }
    
    if ([string isKindOfClass:[NSNull class]]) {
        
        return YES;
        
    }
    
    if ([[string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]] length]==0) {
        
        return YES;
        
    }
    
    return NO;
    
}

@end
