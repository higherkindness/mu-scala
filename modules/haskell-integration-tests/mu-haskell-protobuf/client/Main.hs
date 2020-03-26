{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Maybe (fromMaybe)
import Data.Text as T (Text, pack)
import System.Environment (getArgs)
import Text.Read (readMaybe)

import Mu.GRpc.Client.Optics
import Mu.Schema.Optics

import Protocol

main :: IO ()
main = do
  args <- getArgs
  let config = grpcClientConfigSimple (head args) 9123 False
  Right conn <- initGRpc config msgProtoBuf @WeatherService
  case (tail args) of
    ["ping"] -> ping conn
    ["get-forecast", city, days] -> getForecast conn city days
    _ -> putStrLn "Invalid args"

ping :: GRpcConnection WeatherService 'MsgProtoBuf -> IO ()
ping client = do
  client ^. #ping
  putStrLn "pong"

getForecast :: GRpcConnection WeatherService 'MsgProtoBuf -> String -> String -> IO ()
getForecast client city days = do
  resp <- (client ^. #getForecast) req
  putStrLn $ "Got a forecast: " ++ show resp
    where
      c = Just(T.pack city)
      d = readMaybe days
      req = record (c, d)

