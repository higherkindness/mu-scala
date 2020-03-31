{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Aeson as A (ToJSON, encode)
import Data.ByteString.Char8 as BS (unpack)
import Data.ByteString.Lazy as LBS (toStrict)
import Data.Functor.Identity
import Data.List (intercalate)
import Data.Maybe (fromMaybe)
import Data.Text as T (Text, pack, unpack)
import System.Environment (getArgs)
import Text.Read (readMaybe)

import Mu.Adapter.Json
import Mu.GRpc.Client.Optics
import Mu.Schema.Optics

import Network.GRPC.Client

import AvroProtocol

main :: IO ()
main = do
  args <- getArgs
  let config = grpcClientConfigSimple (head args) 9124 False
  Right conn <- initGRpc config msgAvro @WeatherService
  case (tail args) of
    ["ping"] -> ping conn
    ["get-forecast", city, days] -> getForecast conn city days
    _ -> putStrLn "Invalid args"

ping :: GRpcConnection WeatherService 'MsgAvro -> IO ()
ping client = do
  client ^. #ping
  putStrLn "pong"

getForecast :: GRpcConnection WeatherService 'MsgAvro -> String -> String -> IO ()
getForecast client city days = do
  GRpcOk resp <- (client ^. #getForecast) req
  putStrLn $ showGetForecastResponse resp
    where
      c = T.pack city
      d = fromMaybe 5 $ readMaybe days
      req = record (c, d)

  {-
getForecast :: GRpcConnection WeatherService 'MsgAvro -> String -> String -> IO ()
getForecast client city days = do
  GRpcOk resp <- (client ^. #getForecast) req
  putStrLn $ showGetForecastResponse resp
    where
      c = T.pack city
      d = fromMaybe 5 $ readMaybe days
      req = record (c, d)
  -}

showGetForecastResponse :: GetForecastResponse -> String
showGetForecastResponse resp =
  lastUpdated ++ " " ++ dailyForecasts
    where
      lastUpdated = T.unpack(resp ^. #last_updated)
      dailyForecasts = toJsonString (resp ^. #daily_forecasts)

toJsonString :: A.ToJSON a => a -> String
toJsonString = BS.unpack . LBS.toStrict . A.encode
