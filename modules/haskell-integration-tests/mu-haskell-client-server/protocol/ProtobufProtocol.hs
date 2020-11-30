{-# language DataKinds             #-}
{-# language DeriveAnyClass        #-}
{-# language DeriveGeneric         #-}
{-# language DuplicateRecordFields #-}
{-# language FlexibleContexts      #-}
{-# language FlexibleInstances     #-}
{-# language MultiParamTypeClasses #-}
{-# language PolyKinds             #-}
{-# language TemplateHaskell       #-}
{-# language TypeApplications      #-}
{-# language TypeFamilies          #-}
{-# language TypeOperators         #-}

module ProtobufProtocol where

import           Data.Text                     as T
import           GHC.Generics

import           Mu.Quasi.GRpc
import           Mu.Schema
import           Mu.Schema.Optics

grpc "WeatherProtocol" id "weather.proto"

type GetForecastRequest
  = Term WeatherProtocol (WeatherProtocol :/: "GetForecastRequest")

type Weather = Term WeatherProtocol (WeatherProtocol :/: "GetForecastResponse.Weather")

type GetForecastResponse
  = Term WeatherProtocol (WeatherProtocol :/: "GetForecastResponse")

type RainEvent = Term WeatherProtocol (WeatherProtocol :/: "RainEvent")

type RainEventType = Term WeatherProtocol (WeatherProtocol :/: "RainEvent.RainEventType")

started :: RainEventType
started = enum @"STARTED"

stopped :: RainEventType
stopped = enum @"STOPPED"

type RainSummaryResponse
  = Term WeatherProtocol (WeatherProtocol :/: "RainSummaryResponse")

type SubscribeToRainEventsRequest
  = Term WeatherProtocol (WeatherProtocol :/: "SubscribeToRainEventsRequest")

