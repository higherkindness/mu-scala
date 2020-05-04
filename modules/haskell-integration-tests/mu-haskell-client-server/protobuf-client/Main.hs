{-# language DataKinds             #-}
{-# language FlexibleContexts      #-}
{-# language OverloadedLabels      #-}
{-# language TypeApplications      #-}

module Main where

import           Data.Aeson                    as A
                                                ( ToJSON
                                                , encode
                                                )
import           Data.ByteString.Char8         as BS
                                                ( unpack )
import           Data.ByteString.Lazy          as LBS
                                                ( toStrict )
import           Data.Conduit
import qualified Data.Conduit.Combinators      as C
import           Data.List                      ( intercalate )
import           Data.Maybe                     ( fromMaybe )
import           Data.Text                     as T
                                                ( Text
                                                , pack
                                                , unpack
                                                )
import           System.Environment             ( getArgs )
import           Text.Read                      ( readMaybe )

import           Mu.Adapter.Json
import           Mu.GRpc.Client.Optics
import           Mu.Schema.Optics

import           Network.GRPC.Client

import           ProtobufProtocol

main :: IO ()
main = do
  args <- getArgs
  let config = grpcClientConfigSimple (head args) 9123 False
  Right conn <- initGRpc config msgProtoBuf @WeatherService
  case tail args of
    ["ping"]                     -> ping conn
    ["get-forecast", city, days] -> getForecast conn city days
    ["publish-rain-events", city] -> publishRainEvents conn city
    ["subscribe-to-rain-events", city] -> subscribeToRainEvents conn city
    _                            -> putStrLn "Invalid args"

ping :: GRpcConnection WeatherService 'MsgProtoBuf -> IO ()
ping client = do
  client ^. #ping
  putStrLn "pong"

getForecast
  :: GRpcConnection WeatherService 'MsgProtoBuf -> String -> String -> IO ()
getForecast client city days = do
  GRpcOk resp <- (client ^. #getForecast) req
  putStrLn $ showGetForecastResponse resp
 where
  c   = T.pack city
  d   = fromMaybe 5 $ readMaybe days
  req = record (c, d)

showGetForecastResponse :: GetForecastResponse -> String
showGetForecastResponse resp = lastUpdated ++ " " ++ dailyForecasts
 where
  lastUpdated    = T.unpack $ resp ^. #last_updated
  dailyForecasts = toJsonString $ resp ^. #daily_forecasts

publishRainEvents
  :: GRpcConnection WeatherService 'MsgProtoBuf -> String -> IO ()
publishRainEvents client city = do
  sink        <- (client ^. #publishRainEvents) Compressed
  GRpcOk resp <- runConduit $ stream .| sink
  putStrLn $ showResponse (resp ^. #rained_count)
 where
  stream = C.yieldMany events
  events = toRainEvent <$> [started, stopped, started, stopped, started]
  toRainEvent x = record (T.pack city, Just x)
  showResponse rainedCount =
    "It started raining " ++ show rainedCount ++ " times"

subscribeToRainEvents
  :: GRpcConnection WeatherService 'MsgProtoBuf -> String -> IO ()
subscribeToRainEvents client city = do
  source <- (client ^. #subscribeToRainEvents) req
  runConduit
    $  source
    .| C.map (toJsonString . extractEventType)
    .| C.mapM_ putStrLn
 where
  req = record1 $ T.pack city
  extractEventType (GRpcOk reply) = reply ^. #event_type

toJsonString :: A.ToJSON a => a -> String
toJsonString = BS.unpack . LBS.toStrict . A.encode
