package com.apple.wwrc.foundation;

import com.apple.wwrc.foundation.framework.annotation.ApiParam;
import com.apple.wwrc.foundation.framework.annotation.POSApi;
import com.apple.wwrc.foundation.framework.annotation.POSMethod;
import com.apple.wwrc.foundation.framework.annotation.POSService;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;

/**
 *  This is your service common class. Make sure it's documented here.
 */
@POSService(name = "wwrc-authorization-server", version = "1.0.0")
public interface ServiceInterface {

  // exposed public APIs (Will e availabe at gateway)

  @POSApi(name = "helloWorld", desc = "Hello world api.")
  @POSMethod
  public String helloWorld(@ApiParam(value = "input", desc = "This is sample input") String input) throws FrameworkException;

  @POSApi(name = "add", desc = "Add function, take two value and return two value added together.")
  @POSMethod
  public int add(@ApiParam(value = "x", desc = "X Input") int x,
    @ApiParam(value = "y", desc = "Y Input") int y) throws FrameworkException;

  // non-exposed public APIs

  @POSMethod
  public String addSuffix(String input);
}
