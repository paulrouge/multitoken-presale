#!/usr/bin/env python

import argparse

from scripts.config import Config
from scripts.deploy_contract import deploy


def address_type(string):
    if isinstance(string, str) and len(string) == 42:
        prefix = string[:2]
        if prefix == "hx" or prefix == "cx":
            body_bytes = bytes.fromhex(string[2:])
            body = body_bytes.hex()
            if str(string) == prefix + body:
                return string
    raise argparse.ArgumentTypeError(f"Invalid address: '{string}'")


class Command:

    def __init__(self) -> None:
        parser = argparse.ArgumentParser()
        parser.add_argument('-e', '--endpoint', type=str, default='gochain', help='target endpoint for connection')
        parser.add_argument('-k', '--keystore', type=argparse.FileType('r'), default='res/keystore_gochain',
                            help='keystore file for creating transactions')
        subparsers = parser.add_subparsers(title='Available commands', dest='command')
        subparsers.required = True

        deploy_parser = subparsers.add_parser('deploy')
        deploy_parser.add_argument('contract', type=str, help='target contract to deploy')
        deploy_parser.add_argument('--to', type=address_type, metavar='ADDRESS', help='target address to be updated')

        args = parser.parse_args()
        getattr(self, args.command)(args)

    @staticmethod
    def deploy(args):
        config = Config(args.endpoint, args.keystore.name)
        params = {'TOBEREVEALED_URI':'q','MAX_PRESALES':1000}
        deploy(config, args.contract, args.to, params, verbose=print)


if __name__ == "__main__":
    Command()
