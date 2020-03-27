{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Aeson as A (encode)
import Data.ByteString.Char8 as BS (unpack)
import Data.ByteString.Lazy as LBS (toStrict)
import Data.List (intercalate)
import Data.Maybe (fromMaybe)
import Data.Text as T (Text, pack, unpack)
import System.Environment (getArgs)
import Text.Read (readMaybe)

import Mu.Adapter.Json
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
  GRpcOk resp <- (client ^. #getForecast) req
  putStrLn $ showGetForecastResponse resp
    where
      c = Just(T.pack city)
      d = readMaybe days
      req = record (c, d)

showGetForecastResponse :: GetForecastResponse -> String
showGetForecastResponse resp =
  lastUpdated ++ " " ++ dailyForecasts
    where
      lastUpdated = T.unpack(fromMaybe "" (resp ^. #last_updated))
      dailyForecasts = (BS.unpack . LBS.toStrict . A.encode) (fromMaybe [] (resp ^. #daily_forecasts))
