{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language PartialTypeSignatures #-}
{-# language OverloadedLabels      #-}
{-# language OverloadedStrings     #-}
{-# language TypeApplications      #-}
{-# OPTIONS_GHC -fno-warn-partial-type-signatures #-}

module Main where

import Data.Maybe (fromMaybe)
import Data.Text as T (Text)

import Mu.GRpc.Server
import Mu.Server
import Mu.Schema.Optics

import Protocol

main :: IO ()
main = runGRpcApp msgProtoBuf 9123 server

ping :: (MonadServer m) => m ()
ping = return ()

getForecast :: (MonadServer m) => GetForecastRequest -> m GetForecastResponse
getForecast req =
  return resp
    where
      days      = fromIntegral $ fromMaybe 5 (req ^. #days_required)
      forecasts = sunnyDays days
      resp      = record (Just lastUpdated, Just forecasts)

lastUpdated :: T.Text
lastUpdated = "2020-03-20T12:00:00Z"

sunnyDays :: Int -> [Weather]
sunnyDays n = replicate n (enum @"SUNNY")

server :: (MonadServer m) => ServerT Maybe WeatherService m _
server = Server (ping :<|>: getForecast :<|>: H0)
